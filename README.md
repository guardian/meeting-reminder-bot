# meeting-reminder-bot

If you invite the special google service account to your meeting, and
put a google chat webhook link somewhere in the meeting description,
this lambda will drop a message on your chat channel when the meeting starts.

Ask John for the service account address (it's private to avoid possible abuse)

## Running locally

You should be able to get aws credentials and then run the main method in the LocalTest class (or type `sbt run`)
There are no unit tests or integration tests yet.

## deploying to code quickly

TODO this can be made into an sbt command e.g. `sbt deployToCode` or a shell script

sbt lambda/assembly
cdk synth --app "sbt 'cdk/runMain com.gu.meeting.main'"
aws s3 cp lambda/target/scala-3.6.4/meeting-reminder-bot.jar s3://developer-playground-dist/CODE/meeting-reminder-bot/meeting-reminder-bot.jar --profile developerPlayground
TODO some command here to update the CDK and refresh the lambda code...
