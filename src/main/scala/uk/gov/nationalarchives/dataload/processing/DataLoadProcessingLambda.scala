package uk.gov.nationalarchives.dataload.processing

import graphql.codegen.AddFilesAndMetadata.addFilesAndMetadata.AddFilesAndMetadata
import graphql.codegen.types.{AddOrUpdateFileMetadata, AddOrUpdateMetadata, ClientSideMetadataInput}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.{Json, ParsingFailure, parser}
import uk.gov.nationalarchives.dataload.processing.DataLoadProcessingLambda.{Entry, EventInput, MatchedEntry, getEventInput}
import uk.gov.nationalarchives.tdr.schemautils.SchemaUtils

import java.io.InputStream
import java.util.UUID
import scala.io.Source

class DataLoadProcessingLambda {
  private val s3Utils = S3Utils()
  private val mappedPropertyKeys = SchemaUtils.originalKeyToAlternateKeyMapping("sharePointHeader", "tdrDataLoadHeader")

  private def stubAddFilesResponse(entries: List[ClientSideMetadataInput]): List[AddFilesAndMetadata] = for {
    data <- entries.map(e => AddFilesAndMetadata(UUID.randomUUID(), e.matchId))
  } yield data

  private def convertJsonToEntries(metadataJson: Vector[Map[String, Json]]): List[(ClientSideMetadataInput, (Long, List[Entry]))] = {
    metadataJson.map(j => {
      val matchId: Long = j("matchId").as[Long].getOrElse(0)
      val fileSize: Long = j("File_x0020_Size").as[Long].getOrElse(0)
      val originalPath: String = j("FileRef").as[String].getOrElse("")
      val checksum: String = j("SHA256ClientSideChecksum").as[String].getOrElse("")
//      val lastModified: Long = j("").as[Long].getOrElse(0)
      val metadataEntries: List[Entry] = j.flatMap(e => {
        if (e._1 != "matchId") {
          Some(Entry(e._1, e._2.asString.getOrElse("")))
        } else None
      }).toList
      MatchedEntry(ClientSideMetadataInput(originalPath, checksum, 1, fileSize, matchId), matchId -> metadataEntries)
      (ClientSideMetadataInput(originalPath, checksum, 1, fileSize, matchId), matchId -> metadataEntries)
    }).toList
  }

  private def convertToMetadataInput(fileId: UUID, entries: List[Entry]): AddOrUpdateFileMetadata = {
    val metadataEntries = entries.flatMap(e => {
      if (e.PropertyValue != "matchId") {
        val dataLoadPropertyName = mappedPropertyKeys.getOrElse(e.PropertyName, e.PropertyName)
        Some(AddOrUpdateMetadata(dataLoadPropertyName, e.PropertyValue))
      } else None
    })
    AddOrUpdateFileMetadata(fileId, metadataEntries)
  }

  private def copyRecords(matchedEntries: List[AddFilesAndMetadata]): Unit = {
    //Copy records to the correct s3 bucket key for backend checks to pick up
    //Tag the records for clean up
    matchedEntries.foreach(i => {
      val matchId = i.matchId
      val fileId = i.fileId
    })
  }

  def processDataLoad(inputStream: InputStream): Either[ParsingFailure, List[AddOrUpdateFileMetadata]] = {
    val inputString = Source.fromInputStream(inputStream).mkString
    val input = getEventInput(inputString)
    for {
      //Get the aggregated metadata sidecars json
      sourceJson <- parser.parse(getSourceMetadata(input))
      metadataJson = sourceJson.asArray.get.map(_.asObject.get.toMap)
      //iterate over json and convert to objects mapped to the matchId
      matchedEntries = convertJsonToEntries(metadataJson)
      //add the entries to DB and get fileId to matchId map
      createRecordEntries = stubAddFilesResponse(matchedEntries.map(_._1)).groupBy(_.matchId)
      _ = copyRecords(createRecordEntries.flatMap(_._2).toList)
      //convert the metadata properties to DB input type
      //will need to filter between 'editable' and 'non-editable'
      //'editable' should go to CSV for draft metadata consumption
      metadataInput = matchedEntries.map(_._2).map(i => {
        val fileId = createRecordEntries(i._1).map(_.fileId).head
        convertToMetadataInput(fileId, i._2)
      })
    } yield metadataInput
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
  case class MatchedEntry(clientSideMetadataInput: ClientSideMetadataInput, matchedEntries: (Long, List[Entry]))

  def getEventInput(inputString: String): EventInput = {
    decode[EventInput](inputString) match {
      case Left(error)  => throw new RuntimeException(s"Invalid event input: ${error.getMessage}")
      case Right(input) => input
    }
  }
}
