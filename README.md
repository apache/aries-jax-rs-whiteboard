## JAX-RS Whiteboard

[![Build Status](https://builds.apache.org/buildStatus/icon?job=Aries-component-dsl-master)](https://builds.apache.org/job/Aries-JAX-RS-Whiteboard)
![CI Build](https://github.com/apache/aries-jax-rs-whiteboard/workflows/CI%20Build/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.aries.jax.rs/org.apache.aries.jax.rs.whiteboard.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.aries.jax.rs%22%20AND%20a:%22org.apache.aries.jax.rs.whiteboard%22)

Aries JAX-RS Whiteboard is the reference implementation of the [OSGi JAX-RS Services Whiteboard 1.0](https://osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html).


## Configuration

The whiteboard is configured using configuration admin.

PID                                        | Purpose
-------------------------------------------| -------
org.apache.aries.jax.rs.whiteboard.default | Default JAX-RS Whiteboard instance
org.apache.aries.jax.rs.whiteboard         | Factory PID for creating additional JAX-RS Whiteboard instances

Property                                | Default                       | Description
----------------------------------------|-------------------------------|--------------------------------------------------------
enabled                                 | true                          | Enable or disable the whiteboard instance
default.application.base                | /                             | Base for default application
application.base.prefix                 | ""                            | Prefix for application base
osgi.http.whiteboard.target             | (osgi.http.endpoint=*)        | Select the http whiteboard service if there are several
osgi.http.whiteboard.context.select     | _new context per application_ | Select the http whiteboard context to be used
servlet.init.hide-service-list-page     | true                          | Hide the CXF service list
replace.loopback.address.with.localhost | false                         | Replace loopback addresses by localhost (e.g. by UriBuilder)
application.ready.service.filter        |                               | A service that must be present in order for applications to get started

properties existing in the application service, as those in the whiteboard configuration, are used when the servlet contexts and servlets are registered.

## Integrations

The `integrations` folder contains OSGi enabled integrations for a variety of useful libraries that you might want to use with JAX-RS. In many cases these are just adding OSGi lifecycle and configuration to existing JAX-RS enabled libraries.

## Building

Execute the maven tasks `mvn clean install`.

## Running the Example

The file `jax-rs.itests/target/aries-jaxrs-whiteboard.jar` should have been created.

Execute the following command:

```
java -jar jax-rs.itests/target/aries-jaxrs-whiteboard.jar
```

To enable logging create a logback file like so:

```
<configuration>
  <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator">
    <resetJUL>true</resetJUL>
  </contextListener>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="debug">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

and pass it on the command line like so:

```
java -jar jax-rs.itests/target/aries-jaxrs-whiteboard.jar -Dlogback.configurationFile=file:/absolute/path/to/logback.xml
```

## License

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
