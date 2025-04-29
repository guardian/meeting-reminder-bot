package com.gu.meeting

import software.amazon.awscdk.services.events.targets.LambdaFunction
import software.amazon.awscdk.services.events.{Rule, Schedule}
import software.amazon.awscdk.services.iam.{Policy, PolicyStatement}
import software.amazon.awscdk.services.lambda.*
import software.amazon.awscdk.services.logs.{LogGroup, RetentionDays}
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.{App, Duration, Stack, StackProps}
import software.constructs.Construct

import java.lang.reflect.Method
import scala.jdk.CollectionConverters.*

object InfraStack {

  val bucketName = "developer-playground-dist"
  val app = "meeting-reminder-bot"
  val stack = "playground"

  private val lambdaClass: Class[ReminderHandler] = classOf[ReminderHandler]

  val handlerMethod: String = lambdaClass.getMethods.toList.filter(_.getName == "handleRequest") match {
    case single :: Nil => single.getName
    case other => throw new RuntimeException("couldn't find handler method: " + other)
  }

}

class InfraStack(scope: Construct, id: String, stage: String, props: StackProps) extends Stack(scope, id, props) {

  import InfraStack.*

  val bucket = Bucket.fromBucketName(this, app + "-bucket", bucketName)
  val options: BucketOptions = BucketOptions.builder().build()

  val fn = Function.Builder
    .create(this, app)
    .functionName(app + "-" + stage)
    .runtime(Runtime.JAVA_21)
    .memorySize(1024) // MB
    .handler(lambdaClass.getName + "::" + handlerMethod)
    .code(Code.fromBucketV2(bucket, List(stack, stage, app, app + ".jar").mkString("/"), options))
    .timeout(Duration.minutes(1))
    .architecture(Architecture.ARM_64)
    .environment(
      Map(
        "App" -> app,
        "Stack" -> stack,
        "Stage" -> stage,
      ).asJava,
    )
    .logRetention(RetentionDays.TWO_WEEKS)
    .build()

  val rule = Rule.Builder.create(this, "Schedule Rule").schedule(Schedule.rate(Duration.minutes(1))).build
  rule.addTarget(new LambdaFunction(fn))

  val role = fn.getRole

  private val lambdaPolicy: Policy = Policy.Builder
    .create(this, "LambdaPolicy")
    .statements(
      List(
        PolicyStatement.Builder
          .create()
          .actions(
            List(
              "ssm:GetParametersByPath",
            ).asJava,
          )
          .resources(
            List(
              s"arn:aws:ssm:${super.getRegion}:${super.getAccount}:parameter/$stage/$stack/$app",
            ).asJava,
          )
          .build(),
      ).asJava,
    )
    .build()
  role.attachInlinePolicy(lambdaPolicy)
}

object Infra {
  @main
  def main(): Unit = {
    val app = new App
    val stages = List("CODE", "PROD")
    stages.map { stage =>
      new InfraStack(app, s"meeting-reminder-bot-$stage", stage, StackProps.builder.build)
    }
    val result = app.synth
    println("result: " + result)
  }
}
