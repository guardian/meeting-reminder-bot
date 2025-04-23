ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

lazy val scalafmtSettings = Seq(
  scalafmtFilter.withRank(KeyRanks.Invisible) := "diff-dirty",
  (Test / test) := ((Test / test) dependsOn (Test / scalafmtCheckAll)).value,
  (Test / testOnly) := ((Test / testOnly) dependsOn (Test / scalafmtCheckAll)).evaluated,
  (Test / testQuick) := ((Test / testQuick) dependsOn (Test / scalafmtCheckAll)).evaluated,
)

val circeVersion = "0.14.10"
val scalatest = "org.scalatest" %% "scalatest" % "3.2.19" % "test"

lazy val root = (project in file("."))
  .settings(name := "meeting-reminder-bot")
  .aggregate(cdk, lambda)

lazy val cdk = (project in file("cdk"))
  .dependsOn(lambda)
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % "2.191.0",
      scalatest,
    )
  )
  .settings(scalafmtSettings)

lazy val lambda = (project in file("lambda"))
  .settings(
    name := "lambda",
    libraryDependencies ++= Seq(
      "com.google.api-client" % "google-api-client" % "2.7.2",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.39.0",
      "com.google.apis" % "google-api-services-calendar" % "v3-rev20250404-2.0.0",
      "com.gu" %% "simple-configuration-ssm" % "5.1.0",
      "org.jlib" % "jlib-awslambda-logback" % "1.0.0",
      "ch.qos.logback" % "logback-classic" % "1.5.18",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      scalatest,
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    assembly / assemblyJarName := "meeting-reminder-bot.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
        MergeStrategy.singleOrError
      case PathList(ps @ _*) if ps.last == "module-info.class" => MergeStrategy.discard
      case PathList(ps @ _*) if ps.last == "deriving.conf" => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
      case PathList("mime.types") => MergeStrategy.filterDistinctLines
      case PathList("logback.xml") => MergeStrategy.preferProject
      /*
       * AWS SDK v2 includes a codegen-resources directory in each jar, with conflicting names.
       * This appears to be for generating clients from HTTP services.
       * So it's redundant in a binary artefact.
       */
      case PathList("codegen-resources", _*) => MergeStrategy.discard
      case PathList("META-INF", "FastDoubleParser-LICENSE") => MergeStrategy.concat
      case PathList("META-INF", "FastDoubleParser-NOTICE") => MergeStrategy.concat
      case PathList("META-INF", "okio.kotlin_module") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
  .settings(scalafmtSettings)
