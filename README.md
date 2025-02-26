# meeting-reminder-bot

If you invite the special google service account to your meeting, and
put a google chat webhook link somewhere in the meeting description,
this lambda will drop a message on your chat channel when the meeting starts.

Ask John for the service account address (it's private to avoid possible abuse)

## Running locally

You should be able to get aws credentials and then run the main method in the LocalTest class (or type `sbt run`)
There are no unit tests or integration tests yet.