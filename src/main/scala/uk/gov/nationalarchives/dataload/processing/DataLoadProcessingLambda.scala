package uk.gov.nationalarchives.dataload.processing

import cats.effect.IO
import cats.instances.unit
import graphql.codegen.AddFilesAndMetadata.addFilesAndMetadata.AddFilesAndMetadata
import graphql.codegen.types.{AddFileAndMetadataInput, AddOrUpdateFileMetadata, AddOrUpdateMetadata, ClientSideMetadataInput}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.{Json, ParsingFailure, parser}
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend}
import uk.gov.nationalarchives.dataload.processing.ApplicationConfig.{authUrl, clientSecretPath, ssmEndpoint, timeToLiveSecs}
import uk.gov.nationalarchives.dataload.processing.DataLoadProcessingLambda.{Entry, EventInput, MatchedEntry, getEventInput}
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}
import uk.gov.nationalarchives.tdr.schemautils.SchemaUtils

import java.io.InputStream
import java.net.URI
import java.util.UUID
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

class DataLoadProcessingLambda {
  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
  private val s3Utils = S3Utils()
  private val mappedPropertyKeys = SchemaUtils.originalKeyToAlternateKeyMapping("sharePointHeader", "tdrDataLoadHeader")
  implicit val keycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(authUrl, "tdr", timeToLiveSecs)
  private val keycloakUtils = new KeycloakUtils()
  private val graphQlApi: GraphQlApi = GraphQlApi(keycloakUtils)(backend, keycloakDeployment)

  private def convertJsonToEntries(metadataJson: Vector[Map[String, Json]]): List[(ClientSideMetadataInput, (Long, List[Entry]))] = {
    metadataJson
      .map(j => {
        val matchId: Long = j("matchId").as[Long].getOrElse(0)
        val fileSize: Long = j("File_x0020_Size").as[Long].getOrElse(0)
        val originalPath: String = j("FileRef").as[String].getOrElse("")
        val checksum: String = j("SHA256ClientSideChecksum").as[String].getOrElse("")
//      val lastModified: Long = j("").as[Long].getOrElse(0)
        val metadataEntries: List[Entry] = j
          .flatMap(e => {
            if (e._1 != "matchId") {
              Some(Entry(e._1, e._2.toString()))
            } else None
          })
          .toList
        MatchedEntry(ClientSideMetadataInput(originalPath, checksum, 1, fileSize, matchId), matchId -> metadataEntries)
        (ClientSideMetadataInput(originalPath, checksum, 1, fileSize, matchId), matchId -> metadataEntries)
      })
      .toList
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

  def processDataLoad(inputStream: InputStream) = {
    val inputString = Source.fromInputStream(inputStream).mkString
    val input = getEventInput(inputString)
    val clientSecret = getClientSecret(clientSecretPath, ssmEndpoint)
    for {
      sourceMetadata <- IO(getSourceMetadata(input))
      sourceJson <- IO.fromEither(parser.parse(sourceMetadata))
      metadataJson <- IO(sourceJson.asArray.get.map(_.asObject.get.toMap))
      matchedEntries <- IO(convertJsonToEntries(metadataJson))
      fileEntries <- graphQlApi.addFileEntries(
        clientSecret,
        AddFileAndMetadataInput(input.transferId, matchedEntries.map(_._1), None)
      )
      matchedFileEntries = fileEntries.groupBy(_.matchId)
      metadataInput = matchedEntries.map(_._2).map(i => {
        val fileId = matchedFileEntries(i._1).map(_.fileId).head
        convertToMetadataInput(fileId, i._2)
      })
      systemMetadata <- graphQlApi.addOrUpdateBulkFileMetadata(
        input.transferId, clientSecret, metadataInput)
    } yield metadataInput
  }

  private def getSourceMetadata(eventInput: EventInput): String = (for {
    sourceMetadata <- s3Utils.getMetadataJson(eventInput.s3SourceBucket, eventInput.s3SourceKey)
  } yield sourceMetadata) match {
    case Left(error)           => throw new RuntimeException(s"Failed to retrieve source metadata ${eventInput.s3SourceKey}: ${error.getMessage}")
    case Right(sourceMetadata) => sourceMetadata
  }

  private def getClientSecret(secretPath: String, endpoint: String): String = {
    val httpClient = ApacheHttpClient.builder.build
    val ssmClient: SsmClient = SsmClient
      .builder()
      .endpointOverride(URI.create(endpoint))
      .httpClient(httpClient)
      .region(Region.EU_WEST_2)
      .build()
    val getParameterRequest = GetParameterRequest.builder.name(secretPath).withDecryption(true).build
    ssmClient.getParameter(getParameterRequest).parameter().value()
  }
}

object DataLoadProcessingLambda {
  case class EventInput(userId: UUID, transferId: UUID, s3SourceBucket: String, s3SourceKey: String)
  case class Entry(PropertyName: String, PropertyValue: String)
  case class MatchedEntry(clientSideMetadataInput: ClientSideMetadataInput, matchedEntries: (Long, List[Entry]))

  def getEventInput(inputString: String): EventInput = {
    decode[EventInput](inputString) match {
      case Left(error)  => throw new RuntimeException(s"Invalid event input: ${error.getMessage}")
      case Right(input) => input
    }
  }
}
