package uk.gov.nationalarchives.dataload.processing

import graphql.codegen.AddFilesAndMetadata.addFilesAndMetadata.AddFilesAndMetadata
import graphql.codegen.AddOrUpdateBulkFileMetadata.addOrUpdateBulkFileMetadata.Variables
import graphql.codegen.types.{AddOrUpdateFileMetadata, AddOrUpdateMetadata, ClientSideMetadataInput}
import io.circe.{Json, JsonObject, ParsingFailure, parser}
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.dataload.processing.DataLoadProcessingLambda.{Entry, EventInput, FileEntry, MetadataInput, getEventInput}

import java.io.InputStream
import java.util.UUID
import scala.io.Source

class DataLoadProcessingLambda {
  private val s3Utils = S3Utils()

  private def stubAddFilesResponse(entries: List[ClientSideMetadataInput]): List[AddFilesAndMetadata] = {
    entries.map(e => AddFilesAndMetadata(UUID.randomUUID(), e.matchId))
  }

  def processDataLoad(inputStream: InputStream) = {
    val inputString = Source.fromInputStream(inputStream).mkString
    val input = getEventInput(inputString)
    val sourceJsonString = getSourceMetadata(input)
    val parseResult: Json = parser.parse(sourceJsonString) match {
      case Left(e) => throw e
      case Right(json) => json
    }

    //Get the aggregated metadata sidecars json
    val metadataJson = parseResult.asArray.get.map(_.asObject.get.toMap)
    //Retrieve key mappings
    val keys = metadataJson.flatMap(_.keys).toSet
    //iterate over json and convert to objects mapped to the matchId
    val entries = metadataJson.map(j => {
      val matchId: Long = j("matchId").as[Long].getOrElse(0)
      val fileSize: Long = j("File_x0020_Size").as[Long].getOrElse(0)
      val metadataEntries: List[Entry] = j.flatMap(e => {
        if (e._1 != "matchId") {
          Some(Entry(e._1, e._2.asString.getOrElse("")))
        } else None
      }).toList
      (ClientSideMetadataInput("", "", 1, fileSize, matchId), matchId -> metadataEntries)
    }).toList
    //add the entries to DB and get fileId to matchId map
    val entriesResponse = stubAddFilesResponse(entries.map(_._1)).groupBy(_.matchId)
    //convert the metadata properties to DB input type
    //will need to filter between 'editable' and 'non-editable'
    //'editable' should go to CSV for draft metadata consumption
    val metadataInput = entries.map(_._2).map(e => {
      val fileId = entriesResponse(e._1).map(_.fileId).head
      val metadataEntries = e._2.flatMap(me => {
        if (me.PropertyValue != "matchId") {
          Some(AddOrUpdateMetadata(me.PropertyName, me.PropertyValue))
        } else None
      })
      AddOrUpdateFileMetadata(fileId, metadataEntries)
    })

    sourceJsonString
  }

  private def getSourceMetadata(eventInput: EventInput): String = (for {
    sourceMetadata <- s3Utils.getMetadataJson(eventInput.s3SourceBucket, eventInput.s3SourceKey)
  } yield sourceMetadata) match {
    case Left(error)           => throw new RuntimeException(s"Failed to retrieve source metadata ${eventInput.s3SourceKey}: ${error.getMessage}")
    case Right(sourceMetadata) => sourceMetadata
  }
}

object DataLoadProcessingLambda {
  case class EventInput(userId: UUID, s3SourceBucket: String, s3SourceKey: String)
  case class Entry(PropertyName: String, PropertyValue: String)
  case class FileEntry(matchId: Long, fileSize: Long)
  case class MetadataInput(fileId: UUID, metadata: List[Entry])

  def getEventInput(inputString: String): EventInput = {
    decode[EventInput](inputString) match {
      case Left(error)  => throw new RuntimeException(s"Invalid event input: ${error.getMessage}")
      case Right(input) => input
    }
  }
}
