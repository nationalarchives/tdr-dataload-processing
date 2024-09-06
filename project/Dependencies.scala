import sbt.*

object Dependencies {
  private val circeVersion = "0.14.9"

  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.382"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.173"
  lazy val s3Utils =  "uk.gov.nationalarchives" %% "s3-utils" % "0.1.203"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
}
