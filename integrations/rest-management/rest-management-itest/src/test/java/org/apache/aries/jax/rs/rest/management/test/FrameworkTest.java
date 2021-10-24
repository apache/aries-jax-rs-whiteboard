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

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.osgi.framework.dto.FrameworkDTO;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class FrameworkTest extends TestUtil {

    //@Test
    public void getFrameworkJSON() {
        WebTarget target = createDefaultTarget().path("framework");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("bundles").isArray(),
            j -> j.node("bundles[0]").and(
                j1 -> j1.isObject(),
                j1 -> j1.node("id").isNumber(),
                j1 -> j1.node("lastModified").isNumber(),
                j1 -> j1.node("state").isNumber().isEqualByComparingTo("32"),
                j1 -> j1.node("symbolicName").isString(),
                j1 -> j1.node("version").isString()
            )
        );
    }

    //@Test
    public void getFramework_DOT_JSON() {
        WebTarget target = createDefaultTarget().path("framework.json");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("bundles").isArray(),
            j -> j.node("bundles[0]").and(
                j1 -> j1.isObject(),
                j1 -> j1.node("id").isNumber(),
                j1 -> j1.node("lastModified").isNumber(),
                j1 -> j1.node("state").isNumber().isEqualByComparingTo("32"),
                j1 -> j1.node("symbolicName").isString(),
                j1 -> j1.node("version").isString()
            )
        );
    }

    //@Test
    public void getFrameworkXML() {
        WebTarget target = createDefaultTarget().path("framework");

        Response response = target.request(APPLICATION_XML).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//framework/bundles[1]"
        ).contains("0");
   }

    //@Test
    public void getFrameworkXML_DOT_XML() {
        WebTarget target = createDefaultTarget().path("framework.xml");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//framework/bundles[1]"
        ).contains("0");
   }

    //@Test
    public void getFrameworkDTOJSON() {
        WebTarget target = createDefaultTarget().path("framework");

        Response response = target.request().get();

        FrameworkDTO frameworkDTO = response.readEntity(FrameworkDTO.class);

        assertThat(frameworkDTO.bundles.size()).isGreaterThan(0);
    }

    //@Test
    public void getFrameworkDTOXML() {
        WebTarget target = createDefaultTarget().path("framework");

        Response response = target.request(APPLICATION_XML).get();

        FrameworkDTO frameworkDTO = response.readEntity(FrameworkDTO.class);

        assertThat(frameworkDTO.bundles.size()).isGreaterThan(0);
    }

}
