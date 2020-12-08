# Gateway
API gateway is a facade to hide micro services from end users.
Under the hook, it receives HTTP requests from clients (e.g., mvn) and
forward to different services.

There is a classifier to determine the target service via Url
patterns and HTTP methods.
For example, "/api/content" go to indy and "/api/promote" go to promotion.

## Prerequisite
1. jdk11
2. mvn 3.6.2+

## Configure proxy services

Gateway uses a config file to define proxy services. The configuration is in
'proxy.yaml', e.g.,

```
proxy:
    services:
        - host: indy-infra-nos-automation.cloud.paas.psi.redhat.com
          port: 80
          path-pattern: /api/.+"
```

When receiving a HTTP request, it matches its **path and method**
against the rules and relay the request to target service. If no service found,
it returns a 400 Bad Request with message body **Service not found**.

## Try it

There are a few steps to set it up.

1. Build (make sure you use jdk11 and mvn 3.6.2+)
```
$ git clone git@github.com:Commonjava/gateway.git
$ cd gateway
$ mvn clean compile
```

2. Create ./target/proxy.yaml and copy the above rules to it.

3. Start gateway in debug mode
```
$ mvn quarkus:dev
```

4. Open another terminal and try some maven build, e.g,
```
$ mvn clean install -s <path-to>/settings-gateway.xml
```
The settings-gateway.xml is at the project root directory for test
purpose.
