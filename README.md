# Gateway
API gateway (or simply 'Gateway') is a facade to hide micro services from end users.
It receives HTTP requests from clients and forwards to different services.

Gateway determine the target service via Url patterns and HTTP methods.
For example, "/api/content" goes to indy, and "/api/promote" goes to promotion.

## Prerequisite
1. jdk11
2. mvn 3.6.2+

## Configure proxy services
Gateway uses a config file to define proxy rules. The configuration is in
'proxy.yaml', e.g.,

```
proxy:
  services:
    - host: repo1.maven.org
      ssl: true
      port: 80
      path-pattern: /maven2/.+
```

When receiving a HTTP request, it matches the **path and method**
against the rules and relay the request to target service. If not found,
it returns a 400 Bad Request with message body **Service not found**.

## Try it!
1. Build (make sure you use jdk11 and mvn 3.6.2+)
```
$ git clone git@github.com:Commonjava/gateway.git
$ cd gateway
$ mvn clean compile
```

2. Start gateway in debug mode
```
$ mvn quarkus:dev
```

3. Open another terminal and try something like,
```
curl -v http://localhost:8080/maven2/org/commonjava/util/o11yphant-metrics-core/1.0/o11yphant-metrics-core-1.0.pom
```
Or do a maven build, e.g,
```
$ mvn clean install -s <path-to>/settings-gateway.xml
```
ps. The settings-gateway.xml is at the project root directory for test purpose.

## Caching (Advanced)

Gateway provides cache for downloaded files, e.g,
```
proxy:
  services:
    - host: repo1.maven.org
      ...
      cache:
        enabled: true
        readonly: false
        pattern: ".+\\.(pom|jar)(\\.(md5|sha.+))?$" # only cache jar and pom files
        expire: 2h
        dir: /tmp/cache
```

Cache config is under each service. It is optional.

You can specify arguments like expire (e.g, 1d, 2h, 3m, 30s) or pattern
(for what files to cache). The default cache dir is "${user_dir}/cache"
if not specified explicitly.

The cache is r/w-able. It writes the downloaded files to cache and render them next time.
However you can specify readonly to true so that it does not write the downloaded
files but rather just use whatever pre-installed in that dir.
