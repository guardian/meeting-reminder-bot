package com.gu.meeting

import com.gu.meeting.clients.Config.config
import com.gu.meeting.clients.GCal.calendar
import com.typesafe.scalalogging.StrictLogging

import java.net.http.HttpClient
import java.time.OffsetDateTime

object LocalTest {

  // this main method is useful to try it locally:
  // 1. get dev playground read only credentials
  // 2. add a test meeting to the sandbox calendar
  // 3. update lambdaWarmStartTime to the time of your test meeting
  // 4. run this with `sbt lambda/run`
  @main
  def runTest() = {
    val lambdaWarmStartTime = OffsetDateTime.parse("2025-04-29T09:00:12.123+01:00")
    val googleServiceEmail = config.getString("google-service-email")
    ReminderHandlerSteps.runSteps(googleServiceEmail, calendar, lambdaWarmStartTime, HttpClient.newHttpClient)
  }

}

object ReminderHandler {
  // reuse static http client
  private val client = HttpClient.newHttpClient

  val lambdaColdStartTime = OffsetDateTime.now() // parse("2025-02-13T12:30:12.123Z")

}

class ReminderHandler extends StrictLogging {

  import ReminderHandler.{client, lambdaColdStartTime}
  import com.gu.meeting.clients.Config.*
  import com.gu.meeting.clients.GCal.calendar

  // main runtime entry point
  def handleRequest(): Unit = {
    // have to watch if the lambda starts around the minute, it might not run exactly once in each minute
    val lambdaWarmStartTime = OffsetDateTime.now() // parse("2025-02-13T12:30:12.123Z")
    logger.info(s"starting lambda at $lambdaWarmStartTime, cold start was at $lambdaColdStartTime")
    val googleServiceEmail = config.getString("google-service-email")
    ReminderHandlerSteps.runSteps(googleServiceEmail, calendar, lambdaWarmStartTime, client)
  }
}
