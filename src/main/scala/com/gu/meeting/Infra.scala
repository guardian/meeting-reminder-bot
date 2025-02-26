package com.gu.meeting

import software.amazon.awscdk.services.events.targets.LambdaFunction
import software.amazon.awscdk.services.events.{Rule, Schedule}
import software.amazon.awscdk.services.iam.{Policy, PolicyStatement}
import software.amazon.awscdk.services.lambda.*
import software.constructs.Construct
import software.amazon.awscdk.{App, Duration, Environment, Stack, StackProps}
import software.amazon.awscdk.services.kms.Key
import software.amazon.awscdk.services.logs.{LogGroup, RetentionDays}
import software.amazon.awscdk.services.s3.Bucket

import java.lang.reflect.Method
import scala.jdk.CollectionConverters.*

class InfraStack(scope: Construct, id: String, stage: String, props: StackProps) extends Stack(scope, id, props) {

  val bucketName = "developer-playground-dist"
  val app = "meeting-reminder-bot"
  val stack = "playground"

  val bucket = new Bucket(this, bucketName)
  val options: BucketOptions = BucketOptions.builder().build()

  val myLogGroup = LogGroup.Builder.create(this, "MyLogGroupWithLogGroupName")
    .logGroupName("/aws/lambda/" + id)
    .retention(RetentionDays.TWO_WEEKS)
    .build

  private val lambdaClass: Class[ReminderHandler] = classOf[ReminderHandler]

  private val method: String = lambdaClass.getMethods.toList.filter(_.getName == "handleRequest") match {
    case single :: Nil => single.getName
    case other => throw new RuntimeException("couldn't find handler method: " + other)
  }
  val fn = Function.Builder.create(this, app)
    .runtime(Runtime.JAVA_21)
    .handler(lambdaClass.getName + "." + method)
    .code(Code.fromBucketV2(bucket, List(stage, app, app + ".jar").mkString("/"), options))
    .timeout(Duration.minutes(5))
    .architecture(Architecture.ARM_64)
    .logGroup(myLogGroup)
    .environment(Map(
        "App" -> app,
        "Stack" -> stack,
        "Stage" -> stage,
      ).asJava)
    .build()

  val rule = Rule.Builder.create(this, "Schedule Rule").schedule(Schedule.rate(Duration.minutes(1))).build
  rule.addTarget(new LambdaFunction(fn))

  val role = fn.getRole

  private val lambdaPolicy: Policy = Policy.Builder.create(this, "LambdaPolicy")
    .statements(List(
      PolicyStatement.Builder.create()
        .actions(List(
"ssm:GetParametersByPath"
        ).asJava)
        .resources(List(
          "arn:aws:ssm:${AWS::Region}:${AWS::AccountId}:" + s"$stage/$stack/$app"
        ).asJava)
        .build()
    ).asJava)
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

