package com.gu.meeting

import com.google.api.client.util.DateTime as GDateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.{Event, Events}
import com.gu.meeting.MeetingData.trustedChatBaseUrl
import com.gu.meeting.ChatMessage.chatMessagesForEvents
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import io.circe.Encoder
import io.circe.syntax.*

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.Locale
import scala.jdk.CollectionConverters.*
import scala.util.Try

object ReminderHandlerSteps extends StrictLogging {
  def runSteps(googleServiceEmail: String, calendar: Calendar, now: OffsetDateTime, client: HttpClient): Unit = {

    logger.info(s"Getting events from calendar $googleServiceEmail")

    val events: List[MeetingData] =
      MeetingList.fromEvents(
        calendar.events
          .list(googleServiceEmail)
          .setMaxResults(20)
          .setTimeMin(new GDateTime(now.toEpochSecond * 1000))
          .setOrderBy("startTime")
          .setSingleEvents(true)
          .setTimeZone("Europe/London")
          .execute,
      )

    val minuteOfInterest = now.withSecond(0).withNano(0)

    for {
      (webHookUrl, chatMessage) <- chatMessagesForEvents(events, minuteOfInterest)
    } {
      val request = HttpRequest
        .newBuilder(URI.create(webHookUrl))
        .header("accept", "application/json; charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(chatMessage.asJson.spaces4))
        .build

      val response = client.send(request, HttpResponse.BodyHandlers.ofString)
      logger.info("response from chathook:\n  " + response.body)
    }

  }

}

object MeetingList extends LazyLogging {
  def fromEvents(events: Events): List[MeetingData] = {
    for {
      event <- events.getItems.asScala.toList
      _ = logger.info("got event " + event.getSummary)
      meeting <- MeetingData.fromApiEvent(event)
    } yield meeting
  }
}
