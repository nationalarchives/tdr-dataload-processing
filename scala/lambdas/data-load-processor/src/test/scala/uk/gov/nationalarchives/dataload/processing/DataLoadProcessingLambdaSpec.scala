package uk.gov.nationalarchives.dataload.processing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class DataLoadProcessingLambdaSpec extends AnyFlatSpec {
  "processDataLoad" should "return 'Hello world' string" in {
    val result = new DataLoadProcessingLambda().processDataLoad()
    result shouldBe "Hello world"
  }
}
