package uk.gov.nationalarchives.dataload.processing

import io.circe.Json
import uk.gov.nationalarchives.dataload.processing.MetadataProcessingLambda.{Input, Result}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import graphql.codegen.AddFilesAndMetadata.{addFilesAndMetadata => afmd}

import java.io.{InputStream, OutputStream}
import java.util.UUID
import scala.io.Source

/*Lambda will:
* 1. Read the provided metadata json sidecar
* 2. Creates a file entry in the TDR DB for the record
* 3. Adds any 'non-editable' metadata to the TDR DB
* 4. Writes any editable metadata to a new metadata json sidecar for onward processing
*    - as part of this process the metadata keys will be converted to the alternate keys for draft metadata
* 5. Returns the fileId from the new file entry response
*    - this file id is needs so onward processing can copy the record under the correct s3 bucket key for TDR's backend checks to be triggered*
* */
class MetadataProcessingLambda {

  def processDataLoad(inputStream: InputStream): Unit = {
    val inputString = Source.fromInputStream(inputStream).mkString
    for {
      input <- decode[Input](inputString)
      metadata = getMetadataSideCar(input.s3Bucket, input.s3BucketKey)
      fileEntry = addFileEntry(metadata)
      fileId = fileEntry.fileId
      _ = addNonEditableMetadata(fileId, metadata)
      em = editableMetadata(metadata)
      _ = writeResult(fileId, em)
    } yield Result(fileId, retained())
  }

  //Retrieve metadata sidecar from s3 bucket
  private def getMetadataSideCar(s3Bucket: String, s3BucketKey: String): Nothing = ???

  //Add a file entry to the DB using metadata sidecar values
  private def addFileEntry(metadata: Json): afmd.AddFilesAndMetadata = ???

  //Add any non-editable metadata, ie system metadata. Will need to be defined in the schema
  private def addNonEditableMetadata(fileId: UUID, metadata: Json): Nothing = ???

  //Retrieve any 'editable' metadata. Will need to be defined in the schema
  private def editableMetadata(metadata: Json): Json = ???

  //Write the editable metadata back as a metadata json file for onward processing, ie load into the draft metadata flow (separate lambda)
  private def writeResult(fileId: UUID, editableMetadata: Json) = ???

  //Check if retained
  private def retained(): Boolean = false
}

object  MetadataProcessingLambda {
  case class Input(userId: UUID, s3Bucket: String, s3BucketKey: String)
  case class Result(fileId: UUID, retained: Boolean)
}
