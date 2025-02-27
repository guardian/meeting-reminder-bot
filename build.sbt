ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.3"

val circeVersion = "0.14.10"

lazy val cdk = (project in file("cdk"))
  .dependsOn(lambda)
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % "2.180.0",
    )
  )

lazy val lambda = (project in file("lambda"))
  .settings(
    name := "lambda",
    libraryDependencies ++= Seq(
      "com.google.api-client" % "google-api-client" % "2.7.2",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.38.0",
      "com.google.apis" % "google-api-services-calendar" % "v3-rev20250115-2.0.0",
      "com.gu" %% "simple-configuration-ssm" % "5.0.0",
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
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
