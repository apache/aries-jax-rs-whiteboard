## JAX-RS Whiteboard

[![Build Status](https://builds.apache.org/buildStatus/icon?job=Aries-component-dsl-master)](https://builds.apache.org/job/Aries-JAX-RS-Whiteboard)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.aries.jax.rs/org.apache.aries.jax.rs.whiteboard.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.aries.jax.rs%22%20AND%20a:%22org.apache.aries.jax.rs.whiteboard%22)

Aries JAX-RS Whiteboard is the reference implementation of the [OSGi JAX-RS Services Whiteboard 1.0](https://osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html).

## Integrations

The `integrations` folder contains OSGi enabled integrations for a variety of useful libraries that you might want to use with JAX-RS. In many cases these are just adding OSGi lifecycle and configuration to existing JAX-RS enabled libraries.

## Building

Execute the maven tasks `mvn clean install`.

## Running the Example

The file `jax-rs.example-run/example.jar` should have been created.

Execute the following command:

```
java -jar jax-rs.example-run/example.jar
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
