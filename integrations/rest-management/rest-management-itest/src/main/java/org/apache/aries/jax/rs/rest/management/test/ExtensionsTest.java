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

package org.apache.aries.jax.rs.rest.management.test;

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_EXTENSIONS_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.schema.ExtensionsSchema;
import org.junit.jupiter.api.Test;
import org.osgi.service.rest.RestApiExtension;
import org.osgi.test.common.dictionary.Dictionaries;
import org.osgi.test.common.stream.MapStream;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class ExtensionsTest extends TestUtil {

    @Test
    public void getExtensionsJSON() {
        WebTarget target = createDefaultTarget().path("extensions");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject()
        );
    }

    @Test
    public void getExtensionsJSON_WithService() {
        WebTarget target = createDefaultTarget().path("extensions");

        bundleContext.registerService(
            RestApiExtension.class, new RestApiExtension() {},
            Dictionaries.dictionaryOf(
                RestApiExtension.NAME, "foo",
                RestApiExtension.URI_PATH, "/foo"));

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("extensions").isArray(),
            j -> j.node("extensions[0]").and(
                j1 -> j1.isObject(),
                j1 -> j1.node("name").isEqualTo("foo"),
                j1 -> j1.node("path").isEqualTo("/foo")
            )
        );
    }

    @Test
    public void getExtensions_DOT_JSON() {
        WebTarget target = createDefaultTarget().path("extensions.json");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject()
        );
    }

    @Test
    public void getExtensionsXML() {
        WebTarget target = createDefaultTarget().path("extensions");

        Response response = target.request(APPLICATION_EXTENSIONS_XML_TYPE).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().nodesByXPath(
            "//extensions/*"
        ).isEmpty();
    }

    @Test
    public void getExtensionsXML_DOT_XML() {
        WebTarget target = createDefaultTarget().path("extensions.xml");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().nodesByXPath(
            "//extensions/*"
        ).isEmpty();
    }

    @Test
    public void getExtensionsSchemaJSON() {
        WebTarget target = createDefaultTarget().path("extensions");

        Response response = target.request().get();

        ExtensionsSchema extensionsSchema = response.readEntity(ExtensionsSchema.class);

        assertThat(extensionsSchema.extensions.size()).isEqualTo(0);
    }

    @Test
    public void getExtensionsSchemaXML() {
        WebTarget target = createDefaultTarget().path("extensions");

        Response response = target.request(APPLICATION_EXTENSIONS_XML_TYPE).get();

        ExtensionsSchema extensionsSchema = response.readEntity(ExtensionsSchema.class);

        assertThat(extensionsSchema.extensions.size()).isEqualTo(0);
    }

}
