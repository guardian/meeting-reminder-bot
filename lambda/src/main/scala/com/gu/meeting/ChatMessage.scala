package com.gu.meeting

import com.gu.meeting.ChatMessage.logger
import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder

import java.time.OffsetDateTime

object ChatMessage extends StrictLogging {

  def chatMessagesForEvents(
      events: List[MeetingData],
      minuteOfInterest: OffsetDateTime,
  ): List[(String, ChatMessage)] =
    groupAndFilterAllEvents(events, minuteOfInterest).flatMap(chatMessage)

  def groupAndFilterAllEvents(
      events: List[MeetingData],
      minuteOfInterest: OffsetDateTime,
  ): List[List[MeetingData]] = {
    val instantOfInterest = minuteOfInterest.toInstant
    val endOfDay = minuteOfInterest.withHour(0).plusDays(1).toInstant
    val eventsByCalendar =
      events
        .filter(_.creator.exists(_.endsWith("@guardian.co.uk")))
        .groupBy(_.organiser.map(_._1))
    val upcomingMeetingsByCalendar = eventsByCalendar.view
      .collect { case Some(organiser) -> meetings =>
        val upcomingMeetings =
          meetings
            .dropWhile(_.start.isBefore(instantOfInterest))
            .takeWhile(_.start.isBefore(endOfDay))
        organiser -> upcomingMeetings
      }
    upcomingMeetingsByCalendar.collect {
      case _ -> (nextMeetings @ nextMeeting :: _) if nextMeeting.start == instantOfInterest =>
        logger.info("Sending message for meetings\n  " + nextMeetings.mkString("\n  "))
        nextMeetings
    }.toList
  }

  def chatMessage(upcomingMeetings: List[MeetingData]): Option[(String, ChatMessage)] =
    upcomingMeetings match {
      case currentMeeting :: laterMeetings =>
        import currentMeeting.*
        val link = meetLink match {
          case Some(value) => s"<$value|$title>"
          case None => title
        }
        val suffix = s" is starting at " + formattedStart
        val message = link + suffix
        val calendarName = currentMeeting.organiser.map(_._2).get // we know it's got a value, fix properly later
        val formattedLines = List(
          message,
        ) ++ (if (laterMeetings.nonEmpty)
                List(
                  "",
                  s"*Later $calendarName meetings today*",
                )
              else List()) ++ laterMeetings.map { laterMeeting =>
          s"${laterMeeting.formattedStart} - ${laterMeeting.title}"
        }
        maybeWebHookUrl.map(_ -> ChatMessage(formattedLines.mkString("\n")))
    }

}

case class ChatMessage(
    text: String,
) derives Encoder
