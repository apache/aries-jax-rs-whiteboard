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

package org.apache.aries.jax.rs.rest.management.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.osgi.service.rest.RestApiExtension;

@XmlRootElement(name = "extensions")
public class ExtensionsSchema {

    @XmlElement(name = "extension")
    public List<Extension> extensions = new ArrayList<>();

    public static class Extension {
        public String name;
        public String path;
        public long service;
    }

    public static ExtensionsSchema build(
        Set<CachingServiceReference<RestApiExtension>> extensions) {

        ExtensionsSchema extensionsSchema = new ExtensionsSchema();

        extensions.stream().map(
            ext -> {
                Extension extension = new Extension();
                extension.name = (String)ext.getProperty("org.osgi.rest.name");
                extension.path = (String)ext.getProperty("org.osgi.rest.uri.path");
                Optional.ofNullable(
                    ext.getProperty("org.osgi.rest.service")
                ).map(
                    Long.class::cast
                ).ifPresent(
                    service -> extension.service = service
                );
                return extension;
            }
        ).forEach(extensionsSchema.extensions::add);

        return extensionsSchema;
    }

}
