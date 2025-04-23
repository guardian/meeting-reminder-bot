package com.gu.meeting.clients

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.gu.meeting.clients.Config.config

import java.io.ByteArrayInputStream
import scala.jdk.CollectionConverters.*

object GCal {

  private val gsonFactory = GsonFactory.getDefaultInstance

  private val httpTransport = GoogleNetHttpTransport.newTrustedTransport

  private val scopes = List(CalendarScopes.CALENDAR_READONLY)

  private lazy val serviceAccountCredential: HttpRequestInitializer = {

    val key = "google-api-credential"
    val source = new ByteArrayInputStream(config.getString(key).getBytes("UTF-8"))
    val secret = ServiceAccountCredentials.fromStream(source).createScoped(scopes.asJava)
    source.close()
    // returns an authorized Credential object.
    new HttpCredentialsAdapter(secret)
  }

  val calendar: Calendar = new Calendar.Builder(httpTransport, gsonFactory, serviceAccountCredential)
    .setApplicationName("Meeting chat bot")
    .build()

}
