package uk.gov.nationalarchives.dataload.processing

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import graphql.codegen.AddFilesAndMetadata
import graphql.codegen.AddFilesAndMetadata.{addFilesAndMetadata => afm}
import graphql.codegen.AddOrUpdateBulkFileMetadata.addOrUpdateBulkFileMetadata.AddOrUpdateBulkFileMetadata
import graphql.codegen.AddOrUpdateBulkFileMetadata.{addOrUpdateBulkFileMetadata => aubfm}
import graphql.codegen.types.{AddFileAndMetadataInput, AddOrUpdateBulkFileMetadataInput, AddOrUpdateFileMetadata}
import sttp.client3._
import uk.gov.nationalarchives.dataload.processing.ApplicationConfig.{apiUrl, clientId}
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment, Token}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class GraphQlApi(
                  keycloak: KeycloakUtils,
                  addFilesAndMetadataClient: GraphQLClient[afm.Data, afm.Variables],
                  addOrUpdateBulkFileMetadataClient: GraphQLClient[aubfm.Data, aubfm.Variables])(implicit
    backend: SttpBackend[Identity, Any],
    keycloakDeployment: TdrKeycloakDeployment
) {

  def addFileEntries(clientSecret: String, input: AddFileAndMetadataInput)(implicit
      executionContext: ExecutionContext
  ): IO[List[AddFilesAndMetadata.addFilesAndMetadata.AddFilesAndMetadata]] = for {
    token <- keycloak.serviceAccountToken(clientId, clientSecret).toIO
    result <- addFilesAndMetadataClient.getResult(token, afm.document, afm.Variables(input).some).toIO
    data <- IO.fromOption(result.data)(new RuntimeException("No custom metadata definitions found"))
  } yield data.addFilesAndMetadata

  def addOrUpdateBulkFileMetadata(consignmentId: UUID, clientSecret: String,
                                  fileMetadata: List[AddOrUpdateFileMetadata])
                                 (implicit executionContext: ExecutionContext): IO[List[AddOrUpdateBulkFileMetadata]] =
    for {
      token <- keycloak.serviceAccountToken(clientId, clientSecret).toIO
      metadata <- addOrUpdateBulkFileMetadataClient.getResult(token, aubfm.document, aubfm.Variables(AddOrUpdateBulkFileMetadataInput(consignmentId, fileMetadata)).some).toIO
      data <- IO.fromOption(metadata.data)(new RuntimeException("Unable to add or update bulk file metadata"))
    } yield data.addOrUpdateBulkFileMetadata

  implicit class FutureUtils[T](f: Future[T]) {
    def toIO: IO[T] = IO.fromFuture(IO(f))
  }
}

object GraphQlApi {
  def apply(keycloak: KeycloakUtils)(implicit backend: SttpBackend[Identity, Any], keycloakDeployment: TdrKeycloakDeployment): GraphQlApi = {
    val afmClient = new GraphQLClient[afm.Data, afm.Variables](apiUrl)
    val aubfmClient = new GraphQLClient[aubfm.Data, aubfm.Variables](apiUrl)
    new GraphQlApi(keycloak, afmClient, aubfmClient)
  }
}
