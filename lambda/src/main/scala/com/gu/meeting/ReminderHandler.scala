package com.gu.meeting

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.meeting.clients.Config.config
import com.gu.meeting.clients.GCal.calendar
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider, ProfileCredentialsProvider}

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

object ReminderHandler {
  // reuse static http client
  private val client = HttpClient.newHttpClient

  val lambdaColdStartTime = OffsetDateTime.now() //parse("2025-02-13T12:30:12.123Z")

}

class ReminderHandler extends StrictLogging {

  import com.gu.meeting.clients.Config.*
  import com.gu.meeting.clients.GCal.calendar
  import ReminderHandler.client
  import ReminderHandler.lambdaColdStartTime

  // main runtime entry point
  def handleRequest(): Unit = {
    // have to watch if the lambda starts around the minute, it might not run exactly once in each minute
    val lambdaWarmStartTime = OffsetDateTime.now() //parse("2025-02-13T12:30:12.123Z")
    logger.info(s"starting lambda at $lambdaWarmStartTime, cold start was at $lambdaColdStartTime")
    val googleServiceEmail = config.getString("google-service-email")
    ReminderHandlerSteps.runSteps(googleServiceEmail, calendar, lambdaWarmStartTime, client)
  }
}


