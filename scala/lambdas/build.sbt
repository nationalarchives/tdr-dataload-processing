import Dependencies.*
import uk.gov.nationalarchives.sbt.Log4j2MergePlugin.log4j2MergeStrategy

ThisBuild / organization := "uk.gov.nationalarchives"
name := "lambdas"

lazy val dataLoadLambdasRoot = (project in file("."))
  .aggregate(
    dataLoadProcessor
  )

lazy val commonSettings = Seq(
  name := baseDirectory.value.getName,
  libraryDependencies ++= Seq(
    scalaTest % Test
  ),
  assembly / assemblyOutputPath := file(s"target/outputs/${name.value}"),
  (assembly / assemblyMergeStrategy) := {
    case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" => log4j2MergeStrategy
    case _                                                   => MergeStrategy.first
  },
  scalacOptions ++= Seq("-Wunused:imports", "-Werror", "-deprecation", "-feature", "-language:implicitConversions"),
  (Test / fork) := true,
  (Test / envVars) := Map(
    "AWS_ACCESS_KEY_ID" -> "accesskey",
    "AWS_SECRET_ACCESS_KEY" -> "secret",
    "AWS_LAMBDA_FUNCTION_NAME" -> "test"
  )
)

lazy val dataLoadProcessor = (project in file("data-load-processor"))
  .settings(commonSettings)
  .dependsOn()
  .settings()
