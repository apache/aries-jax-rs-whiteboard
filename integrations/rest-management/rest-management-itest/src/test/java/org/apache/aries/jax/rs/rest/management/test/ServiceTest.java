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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.schema.ServiceSchema;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class ServiceTest extends TestUtil {

    @Test
    public void getServiceJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        Response response = target.resolveTemplate(
            "serviceid", 1l
        ).request(
            APPLICATION_SERVICE_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j1 -> j1.node("id").isNumber().isEqualByComparingTo("1"),
            j1 -> j1.node("properties").and(
                j2 -> j2.isObject(),
                j2 -> j2.node("objectClass").isArray().contains("org.osgi.service.log.LogReaderService", "org.eclipse.equinox.log.ExtendedLogReaderService"),
                j2 -> j2.node("service\\.scope").isString().contains("bundle"),
                j2 -> j2.node("service\\.id").isNumber().isEqualByComparingTo("1"),
                j2 -> j2.node("service\\.bundleid").isNumber().isEqualByComparingTo("0")
            ),
            j1 -> j1.node("bundle").isString().contains("http://", "/rms/framework/bundle/"),
            j1 -> j1.node("usingBundles").isArray()
        );
    }

    @Test
    public void getServiceXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        Response response = target.resolveTemplate(
            "serviceid", 1l
        ).request(
            APPLICATION_SERVICE_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//service/id"
        ).isEqualTo("1");

        XmlAssert.assertThat(
            result
        ).valueByXPath(
            "//service/properties/property[3]/@name"
        ).isEqualTo("service.id");

        XmlAssert.assertThat(
            result
        ).valueByXPath(
            "//service/properties/property[3]/@type"
        ).isEqualTo("Long");

        XmlAssert.assertThat(
            result
        ).valueByXPath(
            "//service/properties/property[3]/@value"
        ).isEqualTo("1");
    }

    @Test
    public void getServiceDTOJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        Response response = target.resolveTemplate(
            "serviceid", 1l
        ).request(
            APPLICATION_SERVICE_JSON_TYPE
        ).get();

        ServiceSchema serviceSchema = response.readEntity(ServiceSchema.class);

        assertThat(
            serviceSchema.id
        ).isEqualTo(
            1l
        );
    }

    @Test
    public void getServiceDTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        Response response = target.resolveTemplate(
            "serviceid", 1l
        ).request(
            APPLICATION_SERVICE_XML_TYPE
        ).get();

        ServiceSchema serviceSchema = response.readEntity(ServiceSchema.class);

        assertThat(
            serviceSchema.id
        ).isEqualTo(
            1l
        );
    }

    //@Test
    public void getService28XML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        Response response = target.resolveTemplate(
            "serviceid", 28l
        ).request(
            APPLICATION_SERVICE_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//service/id"
        ).isEqualTo("28");
    }

    //@Test
    public void getService28DTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        Response response = target.resolveTemplate(
            "serviceid", 28l
        ).request(
            APPLICATION_SERVICE_XML_TYPE
        ).get();

        ServiceSchema serviceSchema = response.readEntity(ServiceSchema.class);

        assertThat(
            serviceSchema.id
        ).isEqualTo(
            28l
        );
    }

}
