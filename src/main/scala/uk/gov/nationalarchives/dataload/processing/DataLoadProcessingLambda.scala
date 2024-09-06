package uk.gov.nationalarchives.dataload.processing

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.dataload.processing.DataLoadProcessingLambda.{Input, getEventInput}

import java.io.InputStream
import java.util.UUID
import scala.io.Source

class DataLoadProcessingLambda {
  private val s3Files = S3Utils()

  def processDataLoad(inputStream: InputStream): Json = {
    val inputString = Source.fromInputStream(inputStream).mkString
    val input = getEventInput(inputString)
    getSourceMetadata(input)
  }

  def getSourceMetadata(input: Input): Json = (for {
    md <- s3Files.getMetadataSidecarJson(input.s3Bucket, input.s3BucketKey)
  } yield md) match {
    case Left(error)                => throw new RuntimeException(s"Failed to retrieve metadata sidecar ${input.s3BucketKey}: ${error.getMessage}")
    case Right(metadataSideCarJson) => metadataSideCarJson
  }
}

object DataLoadProcessingLambda {
  case class Input(userId: UUID, s3Bucket: String, s3BucketKey: String)

  def getEventInput(inputString: String): Input = {
    decode[Input](inputString) match {
      case Left(error)  => throw new RuntimeException(s"Invalid event input: ${error.getMessage}")
      case Right(input) => input
    }
  }
}
