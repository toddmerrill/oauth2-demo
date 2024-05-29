# Welcome to the Oauth2 Demo App!

This app shows a basic Spring Boot Oauth2 authentication flow using GitHub as the authenticator. 
I have deliberately avoided using Spring Security to demonstrate how the flow works.

Very little error handling or testing is added and all the authenticating code is, unlike in a production app, 
located in the DemoController class to provide a single page to see what's happening.

The received access token is used to access the user's GitHub profile. The user name from GitHub is then displayed 
on the page.

## How to run
To run this code locally yourself, you need to create an OAuth2 app in GitHub (in settings/developer settings) 
to receive a client ID and a client secret.

The GitHub client ID and client secret must be passed as environment variables at application startup as:
 - GITHUB_OAUTH_DEMO_CLIENT_ID
 - GITHUB_OAUTH_DEMO_SECRET

1) Build the app as usual with Maven
2) Start the app (Oauth2DemoApplication)
3) Access the app locally at: http://localhost:8990

Enjoy!