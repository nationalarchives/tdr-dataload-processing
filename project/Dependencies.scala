import sbt.*

object Dependencies {
  private val circeVersion = "0.14.9"
  private val mockitoScalaVersion = "1.17.37"

  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val mockitoScala = "org.mockito" %% "mockito-scala" % mockitoScalaVersion
  lazy val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion
  lazy val s3Utils = "uk.gov.nationalarchives" %% "s3-utils" % "0.1.200"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val typeSafeConfig = "com.typesafe" % "config" % "1.4.3"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "3.0.1"
}
