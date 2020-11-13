# Gateway
API gateway works as a facade to hide micro services from end users.
Under the hook, it receives HTTP requests from client (e.g., mvn) and
forward to different services.

There is a classifier which determines the target service via URL
patterns. For example, "/api/content" requests go to indy and
"/api/promote" requests go to promotion.

## Prerequisite
1. jdk11
2. mvn 3.6.2+

## Try it!
$ mvn compile quarkus:dev

This will start Gateway in debug mode. Open another terminal and try
some maven build.

E.g,
mvn clean install -DskipTests -s <path-to>/settings-gateway.xml

The settings-gateway.xml is at the project root directory for test
purpose.

## TODO:

It only supports GET. PUT and POST is yet to come.