package uk.gov.nationalarchives.dataload.processing

import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.io.File
import scala.reflect.io.Directory

class ExternalServicesSpec extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(100, Millis)))

  val wiremockS3 = new WireMockServer(8003)

  override def beforeEach(): Unit = {}

  override def beforeAll(): Unit = {
    wiremockS3.start()
  }

  override def afterAll(): Unit = {
    wiremockS3.stop()
  }

  override def afterEach(): Unit = {
    wiremockS3.resetAll()
  }
}
