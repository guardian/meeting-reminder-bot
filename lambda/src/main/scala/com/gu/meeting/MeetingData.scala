package com.gu.meeting

import com.google.api.services.calendar.model.Event
import com.gu.meeting.MeetingData.{logger, trustedChatBaseUrl}
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.Locale
import scala.jdk.CollectionConverters.*
import scala.util.Try

object MeetingData extends StrictLogging {

  val trustedChatBaseUrl = "https://chat.googleapis.com/" // avoid regex chars.

  def fromApiEvent(event: Event): Option[MeetingData] = {
    val organiser = Option(event.getOrganizer).flatMap(organizer =>
      Option(organizer.getEmail).flatMap(email => Option(email -> organizer.getDisplayName)),
    )
    logger.info(">>  meeting organised by " + organiser)
    val creator = Option(event.getCreator).flatMap(c => Option(c.getEmail))
    logger.info(">>  meeting created by " + creator)
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
    } yield MeetingData(maybeDescription, time, meetLink, title, creator, organiser)
  }
}

case class MeetingData(
    description: Option[String],
    start: Instant,
    meetLink: Option[String],
    title: String,
    creator: Option[String],
    organiser: Option[(String, String)],
) extends LazyLogging {

  val formattedStart: String = OffsetDateTime
    .ofInstant(start, ZoneId.of("Europe/London"))
    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.UK))

  val maybeWebHookUrl: Option[String] = for {
    d <- description
    chatUrl <- d.split(trustedChatBaseUrl).toList match {
      case _ :: link :: _ => Some(trustedChatBaseUrl + link.takeWhile(!List('"', '<', ' ', '\n').contains(_)))
      case _ => None
    }
    webHookUrl = chatUrl.replaceAll("&amp;", "&")
    _ = logger.info(">>  chatUrl: " + webHookUrl)
  } yield webHookUrl

}
