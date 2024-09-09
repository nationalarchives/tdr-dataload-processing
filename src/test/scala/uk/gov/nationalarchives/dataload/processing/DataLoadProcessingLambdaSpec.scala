package uk.gov.nationalarchives.dataload.processing

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.circe.{Json, parser}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Paths}
import scala.io.Source

class DataLoadProcessingLambdaSpec extends ExternalServicesSpec {
  def mockS3GetResponse(fileName: String): StubMapping = {
    val filePath = getClass.getResource(s"/$fileName").getFile
    val bytes = Files.readAllBytes(Paths.get(filePath))
    wiremockS3.stubFor(
      get(urlEqualTo(s"/$fileName"))
        .willReturn(aResponse().withStatus(200).withBody(bytes))
    )
  }

  def getExpectedJson(expectedResultFile: String): Json = parser.parse(Source.fromResource(expectedResultFile).getLines.mkString(System.lineSeparator())) match {
    case Right(json) => json
  }

  "'processDataLoad'" should "throw an error with invalid input event" in {
    val incorrectInputEvent = """{ "user":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586" }"""
    val inputStream = new ByteArrayInputStream(incorrectInputEvent.getBytes())
    val exception = intercept[RuntimeException] {
      new DataLoadProcessingLambda().processDataLoad(inputStream)
    }
    exception.getMessage shouldEqual "Invalid event input: DecodingFailure at .userId: Missing required field"
  }

  "'processDataLoad'" should "return the expected json" in {
    val jsonFileName = "metadata-sidecars.json"
    mockS3GetResponse(jsonFileName)
    val inputEvent = s"""{ "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586", "s3SourceBucket":  "test-bucket", "s3SourceKey" :  "$jsonFileName" }"""
    val inputStream = new ByteArrayInputStream(inputEvent.getBytes())
    val expectedResult = getExpectedJson(jsonFileName)
    val result = new DataLoadProcessingLambda().processDataLoad(inputStream)
    result shouldBe expectedResult
  }

  "'processDataLoad'" should "throw an error if source metadata json is invalid" in {
    val jsonFileName = "invalid.json"
    mockS3GetResponse(jsonFileName)
    val inputEvent = s"""{ "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586", "s3SourceBucket":  "test-bucket", "s3SourceKey" :  "$jsonFileName" }"""
    val inputStream = new ByteArrayInputStream(inputEvent.getBytes())
    val exception = intercept[RuntimeException] {
      new DataLoadProcessingLambda().processDataLoad(inputStream)
    }
    exception.getMessage shouldEqual "Failed to retrieve source metadata invalid.json: expected : got '2001-1...' (line 3, column 40)"
  }

  "'processDataLoad'" should "throw an error if source metadata does not exist" in {
    val inputEvent = """{ "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586", "s3SourceBucket":  "test-bucket", "s3SourceKey" :  "non-existent.json" }"""
    val inputStream = new ByteArrayInputStream(inputEvent.getBytes())
    val exception = intercept[RuntimeException] {
      new DataLoadProcessingLambda().processDataLoad(inputStream)
    }
    exception.getMessage shouldEqual "Failed to retrieve source metadata non-existent.json: null (Service: S3, Status Code: 404, Request ID: null)"
  }
}
