package com.gu.meeting.clients

import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProviderChain,
  EnvironmentVariableCredentialsProvider,
  ProfileCredentialsProvider,
}

object Config extends StrictLogging {

  private val region = "eu-west-1"

  private val ProfileName = "developerPlayground"

  private lazy val credentialsProvider =
    AwsCredentialsProviderChain
      .builder()
      .credentialsProviders(
        ProfileCredentialsProvider.create(ProfileName),
        EnvironmentVariableCredentialsProvider.create(),
      )
      .build()

  lazy val config: Config = {
    logger.info("loading config")
    val isLocal = !sys.env.contains("AWS_SECRET_ACCESS_KEY") // lambda has this set
    val identity =
      if (isLocal)
        DevIdentity("meeting-reminder-bot")
      else
        AppIdentity.whoAmI(defaultAppName = "meeting-reminder-bot", credentialsProvider).get // throw if failed
    val config = ConfigurationLoader.load(identity, credentialsProvider) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      case DevIdentity(myApp) => SSMConfigurationLocation(s"/CODE/playground/$myApp", region)
    }
    logger.info("loaded config")
    config
  }

}
