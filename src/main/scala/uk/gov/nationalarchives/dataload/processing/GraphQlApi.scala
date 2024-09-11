package uk.gov.nationalarchives.dataload.processing

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import graphql.codegen.AddFilesAndMetadata
import graphql.codegen.AddFilesAndMetadata.{addFilesAndMetadata => afm}
import graphql.codegen.types.AddFileAndMetadataInput
import sttp.client3._
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.Token

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class GraphQlApi(addFilesAndMetadataClient: GraphQLClient[afm.Data, afm.Variables])(implicit backend: SttpBackend[Identity, Any]) {

  def addFileEntries(token: Token, input: AddFileAndMetadataInput)(implicit executionContext: ExecutionContext): IO[List[AddFilesAndMetadata.addFilesAndMetadata.AddFilesAndMetadata]] = for {
    result <- addFilesAndMetadataClient.getResult(token.bearerAccessToken, afm.document, afm.Variables(input).some).toIO
    data <- IO.fromOption(result.data)(new RuntimeException("No custom metadata definitions found"))
  } yield data.addFilesAndMetadata

  implicit class FutureUtils[T](f: Future[T]) {
    def toIO: IO[T] = IO.fromFuture(IO(f))
  }
}

object GraphQlApi {
  def apply()(implicit backend: SttpBackend[Identity, Any]): GraphQlApi = {
    val afmClient = new GraphQLClient[afm.Data, afm.Variables]("apiUrl")
    new GraphQlApi(afmClient)
  }
}
