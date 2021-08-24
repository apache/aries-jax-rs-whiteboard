/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.jax.rs.openapi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.jax.rs.whiteboard.ApplicationClasses;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;

import javax.ws.rs.core.Application;

public class JaxrsWhiteboardScanner implements OpenApiScanner {

    private final Application application;
    private final ApplicationClasses applicationClasses;
    public JaxrsWhiteboardScanner(Application application, ApplicationClasses applicationClasses) {
        this.application = application;
        this.applicationClasses = applicationClasses;
    }

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
    }

    @Override
    public Set<Class<?>> classes() {
        Set<Class<?>> classes = new HashSet<>(applicationClasses.classes());
        classes.add(application.getClass());
        return classes;
    }

    @Override
    public Map<String, Object> resources() {
        return null;
    }

}