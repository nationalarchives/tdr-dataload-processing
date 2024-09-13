package uk.gov.nationalarchives.dataload.processing

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.dataload.processing.DataLoadProcessingLambda.{EventInput, getEventInput}

import java.io.InputStream
import java.util.UUID
import scala.io.Source

class DataLoadProcessingLambda {
  private val s3Utils = S3Utils()

  def processDataLoad(inputStream: InputStream): Json = {
    val inputString = Source.fromInputStream(inputStream).mkString
    val input = getEventInput(inputString)
    getSourceMetadata(input)
  }

  private def getSourceMetadata(eventInput: EventInput): Json = (for {
    sourceMetadata <- s3Utils.getMetadataJson(eventInput.s3SourceBucket, eventInput.s3SourceKey)
  } yield sourceMetadata) match {
    case Left(error)           => throw new RuntimeException(s"Failed to retrieve source metadata ${eventInput.s3SourceKey}: ${error.getMessage}")
    case Right(sourceMetadata) => sourceMetadata
  }
}

object DataLoadProcessingLambda {
  case class EventInput(userId: UUID, s3SourceBucket: String, s3SourceKey: String, sourceSystem: String)

  def getEventInput(inputString: String): EventInput = {
    decode[EventInput](inputString) match {
      case Left(error)  => throw new RuntimeException(s"Invalid event input: ${error.getMessage}")
      case Right(input) => input
    }
  }
}
