# Meeting Reminder chat bot

## Tell me how to get a reminder for my meeting!

Follow these two steps:
1. Add a suitable chat webhook to your chat channel (or use an existing one)
<img width="582" alt="image" src="https://github.com/user-attachments/assets/1a956173-0f52-4fd6-9e8a-d794c34ee23d" />

2. Paste the URL into your meeting description somewhere, and invite the special bot to your meeting.  Ask John for the service account address (it's private to avoid possible abuse)
Note: When you add the bot or reschedule a meeting, you don't need to "send out invites" for it to work, but you may need to confirm that you trust the bot as it's not in the guardian.co.uk domain.
<img width="482" alt="image" src="https://github.com/user-attachments/assets/35a5323a-20c8-46f2-bb4d-c4112f96b64b" />

The bot will drop a message on your channel when the meeting starts, respecting any reschedules or cancellations.
<img width="388" alt="image" src="https://github.com/user-attachments/assets/d4874154-7eec-406d-9e99-988148cd667b" />



## How to develop and test the bot

## Running locally

You should be able to get dev playground aws credentials and then run the main method in the LocalTest class (or type `sbt lambda/run`)
Unit tests can be run using `sbt test` (or `sbt cdk/test` and `sbt lambda/test` as required)
TODO There are no runnable integration tests yet.

## CODE env

Currently there is only a CODE env and it is the one in use for sending messages.  In future we can add PROD on CD and make CODE (only) send messages to a test channel.

## deploying to code quickly

TODO make this nicer (this can be made into an sbt command e.g. `sbt deployToCode` or a shell script)

### Lambda Code
1. sbt lambda/assembly
1. aws s3 cp lambda/target/scala-3.6.4/meeting-reminder-bot.jar s3://developer-playground-dist/CODE/meeting-reminder-bot/meeting-reminder-bot.jar --profile developerPlayground
1. Go to the lambda console and update the lambda code.

### Infra/CDK
1. cdk synth --app "sbt 'cdk/runMain com.gu.meeting.main'"
1. Go to cloudformation and update the stack, upload the -CODE template from ./cdk.out folder locally.
