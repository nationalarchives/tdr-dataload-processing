package uk.gov.nationalarchives.dataload.processing

import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}

object ApplicationConfig {
  private val configFactory: TypeSafeConfig = ConfigFactory.load
  val s3Endpoint: String = configFactory.getString("s3.endpoint")
}
