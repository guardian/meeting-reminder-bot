package com.gu.meeting

import com.google.api.client.util.DateTime as GDateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.gu.meeting.Config.config
import com.gu.meeting.GCal.calendar

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import io.circe.Encoder
import io.circe.syntax.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

object ReminderHandlerSteps {
  def runSteps(googleServiceEmail: String, calendar: Calendar, now: OffsetDateTime, client: HttpClient): Unit = {

    val events =
      calendar.events.list(googleServiceEmail)
        .setMaxResults(10)
        .setTimeMin(new GDateTime(now.toEpochSecond * 1000))
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute

    val minuteOfInterest = now.withSecond(0).withNano(0)

    val chatMessages =
      for {
        event <- events.getItems.asScala.toList
        meeting <- MeetingData.fromApiEvent(event, now)
        if minuteOfInterest == meeting.start
        chatMessage = {
          import meeting.*
          val link = meetLink match {
            case Some(value) => s"<$value|$title>"
            case None => title
          }
          val suffix = s" is starting at " + start.format(DateTimeFormatter.ofPattern("hh:mm a"))
          val message = link + suffix
          val formattedText = title + suffix
          ChatMessage(message, formattedText)
        }
      } yield meeting.webHookUrl -> chatMessage

    for {
      (webHookUrl, chatMessage) <- chatMessages
    } {
      val request = HttpRequest.newBuilder(URI.create(webHookUrl))
        .header("accept", "application/json; charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(chatMessage.asJson.spaces4))
        .build

      val response = client.send(request, HttpResponse.BodyHandlers.ofString)
      println(response.body)
    }

  }
}

case class ChatMessage(
  text: String,
  formattedText: String
) derives Encoder

case class MeetingData(
  webHookUrl: String,
  start: OffsetDateTime,
  meetLink: Option[String],
  title: String,
)

object MeetingData {
  def fromApiEvent(event: Event, lambdaStartTime: OffsetDateTime): Option[MeetingData] = {
    val start = Option(event.getStart.getDateTime).getOrElse(event.getStart.getDate)
    val offsetDateTime = Try(OffsetDateTime.parse(start.toStringRfc3339))
    println(s"${event.getSummary} ($start / $offsetDateTime)")
    val maybeDescription = Option(event.getDescription)
    println(maybeDescription)
    val trustedChatBaseUrl = "https://chat.googleapis.com/" // avoid regex chars.  Base url is trusted as we send the API key there.
    val webHookUrl = for {
      d <- maybeDescription
      chatUrl <- d.split(trustedChatBaseUrl).toList match {
        case _ :: link :: _ => Some(trustedChatBaseUrl + link.takeWhile(!List('"', '<', ' ', '\n').contains(_)))
        case _ => None
      }
    } yield chatUrl.replaceAll("&amp;", "&")
    println("chatUrl: " + webHookUrl)
    val meetLink = Option(event.getHangoutLink)
    println("meet: " + meetLink)
    val title = Option(event.getSummary).getOrElse("unnamed meeting")
    webHookUrl.map(u => {
      MeetingData(u, offsetDateTime.getOrElse(lambdaStartTime), meetLink, title)
    })
  }
}
