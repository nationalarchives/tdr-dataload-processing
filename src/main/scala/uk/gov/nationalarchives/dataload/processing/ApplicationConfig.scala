package uk.gov.nationalarchives.dataload.processing

import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}

object ApplicationConfig {
  private val configFactory: TypeSafeConfig = ConfigFactory.load
  val authUrl: String = configFactory.getString("auth.url")
  val apiUrl: String = configFactory.getString("api.url")
  val clientSecretPath: String = configFactory.getString("auth.clientSecretPath")
  val clientId: String = configFactory.getString("auth.clientId")
  val ssmEndpoint: String = configFactory.getString("ssm.endpoint")
  val s3Endpoint: String = configFactory.getString("s3.endpoint")
  val timeToLiveSecs: Int = 60
}
