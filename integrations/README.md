## Integrations

This folder contains OSGi enabled integrations for a variety of useful libraries that you might want to use with JAX-RS. In many cases these are just adding OSGi lifecycle and configuration to existing JAX-RS enabled libraries.

### Building the integration projects

There is a single reactor pom which builds all of the integration projects, however as each project will evolve at different rates they are designed to be built and released separately from the whiteboard and other integration projects.

## Jackson
"[Jackson](https://github.com/FasterXML/jackson) is a suite of data-processing tools for Java (and the JVM platform), including the flagship streaming JSON parser / generator library, matching data-binding library (POJOs to and from JSON) and additional data format modules to process data encoded in Avro, BSON, CBOR, CSV, Smile, (Java) Properties, Protobuf, XML or YAML"

This integration project provides `MessageBodyReader` and `MessageBodyWriter` for automatic (de-)serialization of POJOs within Request-/Responsebodies of JAX-RS Resources that `@Produce/@Consume` the MediaType application/json.

### Configuration

The `MessageBodyReader` and `MessageBodyWriter` can be configured using configuration admin.

PID                                        | 
-------------------------------------------| 
org.apache.aries.jax.rs.jackson            | 

Property                                | Default                       | Description
----------------------------------------|-------------------------------|--------------------------------------------------------
osgi.jaxrs.extension                    | true                          | Marks the MessageBodyReader/-Writer as JAX-RS Extension
osgi.jaxrs.media.type                   | application/json              | registers Reader/Writer for MediaType application/json

You can use other properties defined by the [JAX-RS whiteboard specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html#service.jaxrs.extension.services) to modify the behavior of this integration to your needs.
E.g., you could use `osgi.jaxrs.application.select=(osgi.jaxrs.name=MyApplication)` to make the MessageBodyReader/-Writer available for spcific JAX-RS Applications.

### Setup
In order to make this integration work, you have to deploy the bundle 
```xml
<!-- https://mvnrepository.com/artifact/org.apache.aries.jax.rs/org.apache.aries.jax.rs.jackson -->
<dependency>
    <groupId>org.apache.aries.jax.rs</groupId>
    <artifactId>org.apache.aries.jax.rs.jackson</artifactId>
    <version>1.0.2</version>
</dependency>
```
into your OSGi runtime.


## OpenApi
OpenAPI Specification (formerly Swagger Specification) is an API description format for REST APIs. An OpenAPI file allows you to describe your entire API, including:
* Available endpoints (/users) and operations on each endpoint (GET /users, POST /users)
* Operation parameters Input and output for each operation
* Authentication methods
* Contact information, license, terms of use and other information.
API specifications can be written in YAML or JSON. The complete OpenAPI Specification can be found on GitHub: [OpenAPI 3.0 Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md)

This integration project makes it easy to generate OpenAPI 3.0 compliant json/yaml files for all your JAX-RS Resources and provides an additional JAX-RS Resource that can be called to retrieve the generated json/yaml

### Configuration
The OpenApi integration can be configured by setting configuration properties vie the OpenAPI service (see setup instructions below).

You can use JAX-RS Resource properties defined by the [JAX-RS whiteboard specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html#service.jaxrs.resource.services) to modify the behavior of this integration to your needs.
E.g., you could use `osgi.jaxrs.application.select=(osgi.jaxrs.name=MyApplication)` to generate the OpenAPI json document for a specific JAX-RS Applications.

### Setup
Currently there is no artifact deployed to Maven Central -> This will change in the near future.
Until then you can build the project and use the generated `org.apache.aries.jax.rs.openapi` artifact and deploy it into your OSGi runtime.
Depending on the bundles already installed in your OSGi runtime you may also need to install the openapi dependcies:

```xml
<!-- swagger (transitive) dependencies -->
<bundle>mvn:org.apache.commons/commons-lang3/3.8.1</bundle>
<bundle>mvn:javax.validation/validation-api/1.1.0.Final</bundle>
<bundle>mvn:com.fasterxml.jackson.datatype/jackson-datatype-jsr310/2.11.2</bundle>
<bundle>mvn:org.yaml/snakeyaml/1.27</bundle>
<bundle>mvn:com.fasterxml.jackson.dataformat/jackson-dataformat-yaml/2.11.2</bundle>
<bundle>mvn:io.github.classgraph/classgraph/4.8.92</bundle>

<!-- last swagger version using javax namespace instead of jakarta -->
<bundle>mvn:io.swagger.core.v3/swagger-annotations/2.1.1</bundle>
<bundle>mvn:io.swagger.core.v3/swagger-models/2.1.1</bundle>
<bundle>mvn:io.swagger.core.v3/swagger-core/2.1.1</bundle>
<bundle>mvn:io.swagger.core.v3/swagger-integration/2.1.1</bundle>
<bundle>mvn:io.swagger.core.v3/swagger-jaxrs2/2.1.1</bundle>
```

In order to make this integration work you also have to register one `OpenAPI` service like this:

```java
@Component
public class MyOpenAPI {

    private ServiceRegistration<OpenAPI> registration;

    @Activate
    private void acitvate(BundleContext context){
        OpenAPI api = new OpenAPI();
        Hashtable<String, Object> props = new Hashtable<>();
        // This is optional, if not needed then just use an empty Hashtable as props
        // props.put(JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=MyApplication)");
        registration = context.registerService(OpenAPI.class, api, props);
    }

    @Deactivate
    private void deactivate(){
        registration.unregister();
    }
}
```
From now on you can fetch the generated json/yaml for all your registered JAX-RS Resources (of the default JAX-RS Application) under `https://<your-host>/openapi.json` or `https://<your-host>/<your-app-base-path>/openapi.json` if you are using a custom JAX-RS Application.

## Apache Shiro

[Apache Shiro](https://shiro.apache.org) is an authentication and authorization framework. This integration provides:

Authentication:

* Support for authenticating users using Apache Shiro
* Cookie based user memory
* Session based logout

Authorization:

* Support for injection of Shiro Security Contexts into your JAX-RS resources
* Support for Shiro authorization annotations on your JAX-RS resources 


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
