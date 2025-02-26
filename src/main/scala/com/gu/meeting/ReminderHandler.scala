package com.gu.meeting

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.meeting.Config.config
import com.gu.meeting.GCal.calendar
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.typesafe.config.Config
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider, ProfileCredentialsProvider}
import software.amazon.awssdk.regions.Region

import java.io.ByteArrayInputStream
import java.net.http.HttpClient
import java.time.OffsetDateTime
import scala.jdk.CollectionConverters.*

object LocalTest {

  // this main method is useful to try it locally
  @main
  def runTest() = {
    val lambdaWarmStartTime = OffsetDateTime.parse("2025-02-13T12:30:12.123Z")
    val googleServiceEmail = config.getString("google-service-email")
    ReminderHandlerSteps.runSteps(googleServiceEmail, calendar, lambdaWarmStartTime, HttpClient.newHttpClient)
  }

}

object Config {

  private val region = Region.EU_WEST_1

  private val ProfileName = "developerPlayground"

  private val credentialsProvider =
    AwsCredentialsProviderChain
      .builder()
      .credentialsProviders(
        ProfileCredentialsProvider.create(ProfileName),
        EnvironmentVariableCredentialsProvider.create(),
      )
      .build()

  val config: Config = {
    val isLocal = !sys.env.contains("AWS_SECRET_ACCESS_KEY") // lambda has this set
    val identity =
      if (isLocal)
        DevIdentity("meeting-reminder-bot")
      else
        AppIdentity.whoAmI(defaultAppName = "meeting-reminder-bot", credentialsProvider).get // throw if failed
    val config = ConfigurationLoader.load(identity, credentialsProvider) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      case DevIdentity(myApp) => SSMConfigurationLocation(s"/CODE/playground/$myApp", region.id())
    }
    config
  }

}

object GCal {

  private val gsonFactory = GsonFactory.getDefaultInstance

  private val httpTransport = GoogleNetHttpTransport.newTrustedTransport

  private val scopes = List(CalendarScopes.CALENDAR_READONLY)


  private lazy val serviceAccountCredential: HttpRequestInitializer = {

    val key = "google-api-credential"
    val source = new ByteArrayInputStream(config.getString(key).getBytes("UTF-8"))
    val secret = ServiceAccountCredentials.fromStream(source).createScoped(scopes.asJava)
    source.close()
    //returns an authorized Credential object.
    new HttpCredentialsAdapter(secret)
  }

  val calendar: Calendar = new Calendar.Builder(httpTransport, gsonFactory, serviceAccountCredential).setApplicationName("Meeting chat bot").build()

}

object ReminderHandler {

  import Config.config
  import GCal.calendar
  private val client = HttpClient.newHttpClient

  // TODO have to watch if the lambda starts around the minute, it might not run exactly once in each minute
  //  val lambdaColdStartTime = OffsetDateTime.now() //parse("2025-02-13T12:30:12.123Z")

}

class ReminderHandler {

  import Config.*
  import ReminderHandler.client
  import GCal.calendar

  // main runtime entry point
  def handleRequest(): Unit = {
    // have to watch if the lambda starts around the minute, it might not run exactly once in each minute
    val lambdaWarmStartTime = OffsetDateTime.now() //parse("2025-02-13T12:30:12.123Z")
    val googleServiceEmail = config.getString("google-service-email")
    ReminderHandlerSteps.runSteps(googleServiceEmail, calendar, lambdaWarmStartTime, client)
  }
}


