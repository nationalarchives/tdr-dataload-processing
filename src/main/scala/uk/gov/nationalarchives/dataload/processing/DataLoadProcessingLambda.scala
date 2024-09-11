package uk.gov.nationalarchives.dataload.processing

import graphql.codegen.AddFilesAndMetadata.addFilesAndMetadata.AddFilesAndMetadata
import io.circe.{Json, JsonObject, ParsingFailure, parser}
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.dataload.processing.DataLoadProcessingLambda.{Entry, EventInput, FileEntry, MetadataInput, getEventInput}

import java.io.InputStream
import java.util.UUID
import scala.io.Source

class DataLoadProcessingLambda {
  private val s3Utils = S3Utils()

  private def stubAddFilesResponse(entries: List[FileEntry]) = {
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

    val metadataJson = parseResult.asArray.get.map(_.asObject.get.toMap)
    val keys = metadataJson.flatMap(_.keys).toSet
    val entries = metadataJson.map(j => {
      val matchId: Long = j("matchId").as[Long].getOrElse(0)
      val fileSize: Long = j("ClientSideFileSize").as[Long].getOrElse(0)
      FileEntry(matchId, fileSize)
    }).toList
    val entriesResponse = stubAddFilesResponse(entries).groupBy(_.matchId)
    val metadata = metadataJson.map(j => {
      val matchId: Long = j("matchId").as[Long].getOrElse(0)
      val fileId = entriesResponse(matchId).map(_.fileId).head
      val metadataEntries: List[Entry] = j.flatMap(e => {
        if (e._1 != "matchId") {
          Some(Entry(e._1, e._2.asString.getOrElse("")))
        } else None
      }).toList
      MetadataInput(fileId, metadataEntries)
    })

    val objs = parseResult.asArray.get.map(j => {
      val md = j.asObject.get.toMap
      val matchId: Long = md("matchId").as[Long].getOrElse(0)
      val fileSize: Long = md("ClientSideFileSize").as[Long].getOrElse(0)
      val entry = FileEntry(matchId, fileSize)
      val metadataEntries = md.map(e => Entry(e._1, e._2.asString.getOrElse("")))
      (entry, metadataEntries)
    }).toList


//    val x = decode[List[Entry]](sourceJson) match {
//      case Right(i) => i
//    }
//    val y = x.groupBy(_.matchId)
//    val z: List[FileEntry] = y.map(e => {
//      val ps = e._2
//      val fileSize = ps.filter(_.PropertyName == "property_integer").head.PropertyValue.toLong
//      FileEntry(e._1, fileSize)
//    }).toList

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
