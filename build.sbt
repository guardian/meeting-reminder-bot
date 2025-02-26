ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.3"

val circeVersion = "0.14.10"

lazy val root = (project in file("."))
  .settings(
    name := "meeting-reminder-bot",
    libraryDependencies ++= Seq(
      "com.google.api-client" % "google-api-client" % "2.7.2",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.38.0",
      "com.google.apis" % "google-api-services-calendar" % "v3-rev20250115-2.0.0",
      "software.amazon.awscdk" % "aws-cdk-lib" % "2.180.0",
      "com.gu" %% "simple-configuration-ssm" % "5.0.0",
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )
