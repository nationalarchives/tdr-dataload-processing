package uk.gov.nationalarchives.dataload.processing

import java.io.{ByteArrayInputStream}

object LambdaRunner extends App {
  val eventInput =
    """{
      |"userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586",
      | "s3SourceBucket":  "some-bucket",
      | "s3SourceKey" :  "some-key.json"
      |}
      |""".stripMargin

  private val eventByteInputStream = new ByteArrayInputStream(eventInput.getBytes())
  new DataLoadProcessingLambda().processDataLoad(eventByteInputStream)
}
