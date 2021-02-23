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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.schema.ServiceSchemaListSchema;
import org.apache.aries.jax.rs.rest.management.schema.ServiceListSchema;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class ServicesTest extends TestUtil {

    @Test
    public void getServicesJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.request(
            APPLICATION_SERVICES_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("services").isArray(),
            j -> j.node("services[0]").isString().contains("http://", "/rms/framework/service/")
        );
    }

    @Test
    public void getServicesXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.request(
            APPLICATION_SERVICES_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//services/uri"
        ).contains("/framework/service/");
    }

    @Test
    public void getServicesDTOJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.request(
            APPLICATION_SERVICES_JSON_TYPE
        ).get();

        ServiceListSchema servicesDTO = response.readEntity(ServiceListSchema.class);

        assertThat(
            servicesDTO.services
        ).hasSizeGreaterThan(60).element(0).asString().contains(
            "framework/service/"
        );
    }

    @Test
    public void getServicesDTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.request(
            APPLICATION_SERVICES_XML_TYPE
        ).get();

        ServiceListSchema servicesDTO = response.readEntity(ServiceListSchema.class);

        assertThat(
            servicesDTO.services
        ).hasSizeGreaterThan(60).element(0).asString().contains(
            "framework/service/"
        );
    }

    //////////

    @Test
    public void getServiceRepresentationsJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        Response response = target.request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("services").isArray(),
            j -> j.node("services[0]").and(
                j1 -> j1.isObject(),
                j1 -> j1.node("id").isNumber(),
                j1 -> j1.node("properties").and(
                    j2 -> j2.isObject(),
                    j2 -> j2.node("objectClass").isArray()
                ),
                j1 -> j1.node("bundle").isString().contains("http://", "/rms/framework/bundle/"),
                j1 -> j1.node("usingBundles").isArray()
            )
        );
    }

    @Test
    public void getServiceRepresentationsXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        Response response = target.request(
            APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().hasXPath(
            "//services/service/id"
        );

        XmlAssert.assertThat(
            result
        ).hasXPath(
            "//services/service/properties"
        );

        XmlAssert.assertThat(
            result
        ).hasXPath(
            "//services/service/bundle"
        );

        XmlAssert.assertThat(
            result
        ).hasXPath(
            "//services/service/usingBundles"
        );
    }

    @Test
    public void getServiceRepresentationsDTOJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        Response response = target.request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        ServiceSchemaListSchema serviceSchemaListSchema = response.readEntity(ServiceSchemaListSchema.class);

        assertThat(
            serviceSchemaListSchema.services
        ).isNotEmpty();
    }

    @Test
    public void getServiceRepresentationsDTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        Response response = target.request(
            APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE
        ).get();

        ServiceSchemaListSchema serviceSchemaListSchema = response.readEntity(ServiceSchemaListSchema.class);

        assertThat(
            serviceSchemaListSchema.services
        ).isNotEmpty();
    }

    ////////////

    @Test
    public void getServicesFilterJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.queryParam(
            "filter", "(service.id=25)"
        ).request(
            APPLICATION_SERVICES_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("services").and(
                j1 -> j1.isArray().hasSize(1).element(0).asString().contains("http://", "/rms/framework/service/25")
            )
        );
    }

    @Test
    public void getServicesFilterXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.queryParam(
            "filter", "(service.id=25)"
        ).request(
            APPLICATION_SERVICES_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//services/uri"
        ).contains("framework/service/25");
    }

    @Test
    public void getServicesFilterDTOJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.queryParam(
            "filter", "(service.id=25)"
        ).request(
            APPLICATION_SERVICES_JSON_TYPE
        ).get();

        ServiceListSchema servicesDTO = response.readEntity(ServiceListSchema.class);

        assertThat(
            servicesDTO.services.get(0)
        ).contains(
            "framework/service/25"
        );
    }

    @Test
    public void getServicesFilterDTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services");

        Response response = target.queryParam(
            "filter", "(service.id=25)"
        ).request(
            APPLICATION_SERVICES_XML_TYPE
        ).get();

        ServiceListSchema servicesDTO = response.readEntity(ServiceListSchema.class);

        assertThat(
            servicesDTO.services.get(0)
        ).contains(
            "framework/service/25"
        );
    }

    @Test
    public void getServicesFilterDTOJSON_2() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        Response response = target.queryParam(
            "filter", "(objectClass=org.eclipse.osgi.*)"
        ).request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        ServiceSchemaListSchema serviceSchemaListSchema = response.readEntity(ServiceSchemaListSchema.class);

        assertThat(
            serviceSchemaListSchema.services.get(0)
        ).hasFieldOrProperty(
            "bundle"
        ).hasFieldOrProperty(
            "id"
        ).hasFieldOrProperty(
            "properties"
        ).hasFieldOrProperty(
            "usingBundles"
        );

        Map<String, Object> properties = serviceSchemaListSchema.services.get(0).properties;

        assertThat(properties).contains(
            entry("objectClass", Arrays.asList("org.eclipse.osgi.framework.log.FrameworkLog"))
        ).contains(
            entry("service.scope", "bundle")
        );
    }

    @Test
    public void getServicesFilterDTOJSON_caseInsensitiveMatching() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        Response response = target.queryParam(
            "filter", "(objectclass=org.eclipse.osgi.*)"
        ).request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        ServiceSchemaListSchema serviceSchemaListSchema = response.readEntity(ServiceSchemaListSchema.class);

        assertThat(
            serviceSchemaListSchema.services.get(0)
        ).hasFieldOrProperty(
            "bundle"
        ).hasFieldOrProperty(
            "id"
        ).hasFieldOrProperty(
            "properties"
        ).hasFieldOrProperty(
            "usingBundles"
        );

        Map<String, Object> properties = serviceSchemaListSchema.services.get(0).properties;

        assertThat(properties).contains(
            entry("objectClass", Arrays.asList("org.eclipse.osgi.framework.log.FrameworkLog"))
        ).contains(
            entry("service.scope", "bundle")
        );
    }

}
