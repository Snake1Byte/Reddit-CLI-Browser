# Reddit-CLI-Browser

To build this on your machine, you need a `constants.json` file in the
`src/com/github/snake1byte/reddit_cli_browser/resources` package. The JSON-file
needs to contain the following object:

```json
{
    "refresh_token": "<Reddit OAuth 2.0 refresh token>",
    "user_agent": "<platform>:<unique app name>:v<version> (by <Reddit username>)",
    "client_id": "<Your Reddit app's client ID>",
    "client_secret": "<Your Reddit app's client secret>"
}
```