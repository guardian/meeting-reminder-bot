package com.gu.meeting

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset}
import ChatMessage.*
import com.gu.meeting.TestData.localDateTime

class MeetingDataTest extends AnyFlatSpec with Matchers {

  import TestData.*

  "formattedStart" should "display the time correctly whether it's BST or GMT" in {
    thisMinuteNoMeet.formattedStart should be("1:00 pm")
    val time = LocalDateTime.of(2025, 1, 1, 13, 0, 0, 0)
    val localDateTime: Instant = time.toInstant(ZoneId.of("Europe/London").getRules.getOffset(time)) // in GMT
    thisMinuteNoMeet.copy(start = localDateTime).formattedStart should be("1:00 pm")
  }

  "maybeWebHookUrl" should "only have a value if there's an embedded link in the description" in {
    thisMinuteNoMeet.maybeWebHookUrl should be(Some("https://chat.googleapis.com/asdfghjk"))
    thisMinuteWithMeet.copy(description = None).maybeWebHookUrl should be(None)
    thisMinuteWithMeet.copy(description = Some("kjhaskjdh")).maybeWebHookUrl should be(None)
  }

}

object TestData {

  private val localDateTime: LocalDateTime = LocalDateTime.of(2025, 4, 23, 13, 0, 0, 0) // in BST
  private val londonOffset: ZoneOffset = ZoneId.of("Europe/London").getRules.getOffset(localDateTime)
  val offsetNow: OffsetDateTime = OffsetDateTime.of(localDateTime, londonOffset)
  val thisInstant: Instant = localDateTime.toInstant(londonOffset)

  val thisMinuteNoMeet: MeetingData = MeetingData(
    description = Some("qwerty https://chat.googleapis.com/asdfghjk zxcvbn"),
    start = thisInstant,
    meetLink = None,
    title = "My Test meeting",
    creator = Some("hello@guardian.co.uk"),
    organiser = Some("srvalue@calendar.google.com" -> "SR: Value"),
  )
  val thisMinuteWithMeet: MeetingData = thisMinuteNoMeet.copy(meetLink = Some("https://meet.google.com/asd-qwer-zxc"))

}
