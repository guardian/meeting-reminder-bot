package com.gu.meeting

import com.gu.meeting.ChatMessage.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChatMessageTest extends AnyFlatSpec with Matchers {

  import TestData.*

  "chatMessage" should "send a message where the meeting HAS NO meet link" in {
    val testData = thisMinuteNoMeet
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      "My Test meeting is starting at 1:00 pm",
    )
    val actual = chatMessage(List(testData))
    actual should be(Some(expected))
  }

  it should "send a message where the meeting HAS A meet link" in {
    val testData = thisMinuteWithMeet
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      "<https://meet.google.com/asd-qwer-zxc|My Test meeting> is starting at 1:00 pm",
    )
    val actual = chatMessage(List(testData))
    actual should be(Some(expected))
  }

  it should "send a message with multiple upcoming meetings" in {
    val testData = List(thisMinuteWithMeet, thisMinuteNoMeet.copy(start = thisMinuteNoMeet.start.plusSeconds(60 * 60)))
    val expected = "https://chat.googleapis.com/asdfghjk" -> ChatMessage(
      """<https://meet.google.com/asd-qwer-zxc|My Test meeting> is starting at 1:00 pm
        |
        |*Later SR: Value meetings today*
        |2:00 pm - My Test meeting""".stripMargin,
    )
    val actual = chatMessage(testData)
    actual should be(Some(expected))
  }

  "groupAndFilterAllEvents" should "send a message where the meeting is now" in {
    val testData = thisMinuteNoMeet
    val actual = groupAndFilterAllEvents(List(testData), offsetNow)
    actual should be(List(List(thisMinuteNoMeet)))
  }

  it should "send no message where the meeting is in one minute" in {
    val testData = thisMinuteNoMeet
    val actual = groupAndFilterAllEvents(List(testData), offsetNow.minusSeconds(60))
    actual should be(List.empty)
  }

  it should "aggregate meetings with the same calendar" in {
    val inAnHour = thisMinuteNoMeet.copy(title = "later meeting", start = thisMinuteNoMeet.start.plusSeconds(60 * 60))
    val testData = List(thisMinuteNoMeet, inAnHour)
    val actual = groupAndFilterAllEvents(testData, offsetNow)
    actual should be(List(List(thisMinuteNoMeet, inAnHour)))
  }

  it should "not aggregate meetings earlier or tomorrow" in {
    val anHourAgo =
      thisMinuteNoMeet.copy(title = "earlier meeting", start = thisMinuteNoMeet.start.minusSeconds(60 * 60))
    val tomorrowMeeting =
      thisMinuteNoMeet.copy(title = "tomorrow meeting", start = thisMinuteNoMeet.start.plusSeconds(60 * 60 * 24))
    val testData = List(anHourAgo, thisMinuteNoMeet, tomorrowMeeting)
    val actual = groupAndFilterAllEvents(testData, offsetNow)
    actual should be(List(List(thisMinuteNoMeet)))
  }

  it should "filter out non organisation meetings" in {
    val testData = thisMinuteNoMeet.copy(creator = Some("test@baddies.com"))
    val actual = groupAndFilterAllEvents(List(testData), offsetNow)
    actual should be(List.empty)
  }

}
