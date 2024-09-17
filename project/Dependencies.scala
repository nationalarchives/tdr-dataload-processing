import sbt.*

object Dependencies {
  private val circeVersion = "0.14.10"
  private val mockitoScalaVersion = "1.17.37"

  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.208"
  lazy val awsSsm = "software.amazon.awssdk" % "ssm" % "2.26.25"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.386"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.181"
  lazy val mockitoScala = "org.mockito" %% "mockito-scala" % mockitoScalaVersion
  lazy val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion
  lazy val s3Utils = "uk.gov.nationalarchives" %% "s3-utils" % "0.1.206"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val schemaUtils = "uk.gov.nationalarchives" %% "tdr-schema-utils" %"0.0.54-SNAPSHOT"
  lazy val typeSafeConfig = "com.typesafe" % "config" % "1.4.3"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "3.0.1"
}
