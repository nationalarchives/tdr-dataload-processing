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

  "'processDataLoad'" should "throw an error in incorrect input event" in {
    val incorrectInputEvent = """{ "user":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586" }"""
    val inputStream = new ByteArrayInputStream(incorrectInputEvent.getBytes())
    val exception = intercept[RuntimeException] {
      new DataLoadProcessingLambda().processDataLoad(inputStream)
    }
    exception.getMessage shouldEqual "Invalid event input: DecodingFailure at .userId: Missing required field"
  }

  "'processDataLoad'" should "return the expected json" in {
    mockS3GetResponse("metadata-sidecar.json")
    val inputEvent = """{ "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586", "s3Bucket":  "test-bucket", "s3BucketKey" :  "metadata-sidecar.json" }"""
    val inputStream = new ByteArrayInputStream(inputEvent.getBytes())
    val expectedResult = getExpectedJson("metadata-sidecar.json")
    val result = new DataLoadProcessingLambda().processDataLoad(inputStream)
    result shouldBe expectedResult
  }

  "'processDataLoad'" should "throw an error if sidecar metadata json is invalid" in {
    mockS3GetResponse("invalid.json")
    val inputEvent = """{ "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586", "s3Bucket":  "test-bucket", "s3BucketKey" :  "invalid.json" }"""
    val inputStream = new ByteArrayInputStream(inputEvent.getBytes())
    val exception = intercept[RuntimeException] {
      new DataLoadProcessingLambda().processDataLoad(inputStream)
    }
    exception.getMessage shouldEqual "Failed to retrieve metadata sidecar invalid.json: expected : got '2001-1...' (line 2, column 38)"
  }

  "'processDataLoad'" should "throw an error if sidecar metadata does not exist" in {
    val inputEvent = """{ "userId":  "f0a73877-6057-4bbb-a1eb-7c7b73cab586", "s3Bucket":  "test-bucket", "s3BucketKey" :  "non-existent.json" }"""
    val inputStream = new ByteArrayInputStream(inputEvent.getBytes())
    val exception = intercept[RuntimeException] {
      new DataLoadProcessingLambda().processDataLoad(inputStream)
    }
    exception.getMessage shouldEqual "Failed to retrieve metadata sidecar non-existent.json: null (Service: S3, Status Code: 404, Request ID: null)"
  }
}
