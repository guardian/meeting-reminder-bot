package com.gu.meeting

import com.google.api.client.util.DateTime as GDateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.{Event, Events}
import com.gu.meeting.MeetingData.trustedChatBaseUrl
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

    val events: Events = {
      calendar.events
        .list(googleServiceEmail)
        .setMaxResults(10)
        .setTimeMin(new GDateTime(now.toEpochSecond * 1000))
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .setTimeZone("Europe/London")
        .execute

    }

    val minuteOfInterest = now.withSecond(0).withNano(0).toInstant

    for {
      meeting <- MeetingList.fromEvents(events)
      (webHookUrl, chatMessage) <- meeting.maybeMessage(minuteOfInterest)
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

case class ChatMessage(
    text: String,
    formattedText: String,
) derives Encoder

case class MeetingData(
    description: Option[String],
    start: Instant,
    meetLink: Option[String],
    title: String,
    owner: Option[String],
) extends LazyLogging {

  val chatMessage: ChatMessage = {
    val link = meetLink match {
      case Some(value) => s"<$value|$title>"
      case None => title
    }
    val suffix = s" is starting at " + OffsetDateTime
      .ofInstant(start, ZoneId.of("Europe/London"))
      .format(DateTimeFormatter.ofPattern("h:mm a", Locale.UK))
    val message = link + suffix
    val formattedText = title + suffix
    ChatMessage(message, formattedText)
  }

  val maybeWebHookUrl: Option[String] = for {
    d <- description
    chatUrl <- d.split(trustedChatBaseUrl).toList match {
      case _ :: link :: _ => Some(trustedChatBaseUrl + link.takeWhile(!List('"', '<', ' ', '\n').contains(_)))
      case _ => None
    }
    webHookUrl = chatUrl.replaceAll("&amp;", "&")
    _ = logger.info(">>  chatUrl: " + webHookUrl)
  } yield webHookUrl

  def maybeMessage(minuteOfInterest: Instant): Option[(String, ChatMessage)] =
    for {
      webHookUrl <- maybeWebHookUrl
      if owner.exists(_.endsWith("@guardian.co.uk"))
      _ = logger.info(s"comparing minute of interest $minuteOfInterest with $start")
      if minuteOfInterest == start
      _ = logger.info("Sending message for meeting " + this)
    } yield webHookUrl -> chatMessage
}

object MeetingData extends StrictLogging {

  val trustedChatBaseUrl = "https://chat.googleapis.com/" // avoid regex chars.

  def fromApiEvent(event: Event): Option[MeetingData] = {
    val owner = Option(event.getCreator.getEmail)
    logger.info(">>  meeting owned by " + owner)
    val start = Option(event.getStart.getDateTime).getOrElse(event.getStart.getDate)
    val offsetDateTime = Try(OffsetDateTime.parse(start.toStringRfc3339))
    logger.info(s">>  summary: ${event.getSummary} ($start / $offsetDateTime)")
    val maybeDescription = Option(event.getDescription)
    logger.info(">>  description: " + maybeDescription)
    val meetLink = Option(event.getHangoutLink)
    logger.info(">>  meet: " + meetLink)
    val title = Option(event.getSummary).getOrElse("unnamed meeting")
    for {
      time <- offsetDateTime.toOption.map(_.toInstant)
    } yield MeetingData(maybeDescription, time, meetLink, title, owner)
  }
}
