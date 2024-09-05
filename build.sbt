ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .aggregate(dataLoadLambdasRoot)
  .settings(
    name := "dataload",
    scalaVersion := "2.13.14"
  )

lazy val dataLoadLambdasRoot = project in file("./scala/lambdas")
