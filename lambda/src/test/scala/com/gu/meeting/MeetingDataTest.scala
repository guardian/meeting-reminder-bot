package com.gu.meeting

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, LocalDateTime, ZoneId}

class MeetingDataTest extends AnyFlatSpec with Matchers {
  import TestData.*

  "eventsToMessages" should "send a message where the meeting HAS NO meet link" in {
    val testData = thisMinuteNoMeet
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      "My Test meeting is starting at 1:00 pm",
      "My Test meeting is starting at 1:00 pm",
    )
    val actual = testData.maybeMessage(thisInstant)
    actual should be(Some(expected))
  }

  it should "send a message where the meeting HAS A meet link" in {
    val testData = thisMinuteNoMeet.copy(meetLink = Some("https://meet.google.com/asd-qwer-zxc"))
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      "<https://meet.google.com/asd-qwer-zxc|My Test meeting> is starting at 1:00 pm",
      "My Test meeting is starting at 1:00 pm",
    )
    val actual = testData.maybeMessage(thisInstant)
    actual should be(Some(expected))
  }

  it should "send no message where the meeting is in one minute" in {
    val testData = thisMinuteNoMeet
    val actual = testData.maybeMessage(thisInstant.minusSeconds(60))
    actual should be(None)
  }

  it should "filter out non organisation meetings" in {
    val testData = thisMinuteNoMeet.copy(owner = Some("test@baddies.com"))
    val actual = testData.maybeMessage(thisInstant)
    actual should be(None)
  }

}

object TestData {

  private val localDateTime: LocalDateTime = LocalDateTime.of(2025, 4, 23, 13, 0, 0, 0)
  val thisInstant: Instant = localDateTime.toInstant(ZoneId.of("Europe/London").getRules.getOffset(localDateTime))

  val thisMinuteNoMeet: MeetingData = MeetingData(
    Some("qwerty https://chat.googleapis.com/asdfghjk zxcvbn"),
    thisInstant,
    None,
    "My Test meeting",
    Some("hello@guardian.co.uk"),
  )

}
