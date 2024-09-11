package uk.gov.nationalarchives.dataload.processing

import io.circe.Json
import io.circe.parser.parse
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.gov.nationalarchives.aws.utils.s3.S3Clients.s3
import uk.gov.nationalarchives.dataload.processing.ApplicationConfig.s3Endpoint

import scala.util.Try

class S3Utils(s3Client: S3Client) {
  def getMetadataJson(bucket: String, key: String): Either[Throwable, String] = {
    for {
      s3Response <- Try(s3Client.getObject(GetObjectRequest.builder.bucket(bucket).key(key).build)).toEither
      jsonString = s3Response.readAllBytes().map(_.toChar).mkString
    } yield jsonString
  }
}

object S3Utils {
  def apply() = new S3Utils(s3(s3Endpoint))
}
