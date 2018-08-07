## Integrations

This folder contains OSGi enabled integrations for a variety of useful libraries that you might want to use with JAX-RS. In many cases these are just adding OSGi lifecycle and configuration to existing JAX-RS enabled libraries.

### Building the integration projects

There is a single reactor pom which builds all of the integration projects, however as each project will evolve at different rates they are designed to be built and released separately from the whiteboard and other integration projects.

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
