package com.gu.meeting

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{Event, EventDateTime}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{OffsetDateTime, ZoneOffset}

class MeetingDataTest extends AnyFlatSpec with Matchers {
  import TestData.*

  "eventsToMessages" should "send a message where the meeting HAS NO meet link" in {
    val testData = List(thisMinuteNoMeet)
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      "My Test meeting is starting at 12:00 pm",
      "My Test meeting is starting at 12:00 pm",
    )
    val actual = MeetingData.eventsToMessages(thisMinute, testData)
    actual should be(List(expected))
  }

  it should "send a message where the meeting HAS A meet link" in {
    val testData = List(thisMinuteWithMeet)
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      "<https://meet.google.com/asd-qwer-zxc|My Test meeting> is starting at 12:00 pm",
      "My Test meeting is starting at 12:00 pm",
    )
    val actual = MeetingData.eventsToMessages(thisMinute, testData)
    actual should be(List(expected))
  }

  it should "send no message where the meeting is in one minute" in {
    val testData = List(thisMinuteWithMeet)
    val actual = MeetingData.eventsToMessages(thisMinute.minusMinutes(1), testData)
    actual should be(List.empty)
  }

}

object TestData {

  val thisMinute = OffsetDateTime.of(2025, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC)

  val thisMinuteNoMeet = new Event()
    .setStart(
      new EventDateTime()
        .setDateTime(new DateTime(thisMinute.toEpochSecond * 1000, 0)),
    )
    .setDescription("qwerty https://chat.googleapis.com/asdfghjk zxcvbn")
    .setSummary("My Test meeting")

  val thisMinuteWithMeet =
    thisMinuteNoMeet.clone().setHangoutLink("https://meet.google.com/asd-qwer-zxc")

}
