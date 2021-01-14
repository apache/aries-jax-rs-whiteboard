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
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.handler.RestManagementMessageBodyHandler;
import org.apache.aries.jax.rs.rest.management.model.ServiceReferenceDTOs;
import org.apache.aries.jax.rs.rest.management.model.ServicesDTO;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit4.context.BundleContextRule;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class ServicesTest extends TestUtil {

    @Rule
    public BundleContextRule bundleContextRule = new BundleContextRule();

    @InjectBundleContext
    BundleContext bundleContext;

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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.request(
            APPLICATION_SERVICES_JSON_TYPE
        ).get();

        ServicesDTO servicesDTO = response.readEntity(ServicesDTO.class);

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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.request(
            APPLICATION_SERVICES_XML_TYPE
        ).get();

        ServicesDTO servicesDTO = response.readEntity(ServicesDTO.class);

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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        ServiceReferenceDTOs serviceReferenceDTOs = response.readEntity(ServiceReferenceDTOs.class);

        assertThat(
            serviceReferenceDTOs.services
        ).isNotEmpty();
    }

    @Test
    public void getServiceRepresentationsDTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.request(
            APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE
        ).get();

        ServiceReferenceDTOs serviceReferenceDTOs = response.readEntity(ServiceReferenceDTOs.class);

        assertThat(
            serviceReferenceDTOs.services
        ).isNotEmpty();
    }

    //////////

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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "serviceid", 1l
        ).request(
            APPLICATION_SERVICE_JSON_TYPE
        ).get();

        ServiceReferenceDTO serviceReferenceDTO = response.readEntity(ServiceReferenceDTO.class);

        assertThat(
            serviceReferenceDTO.id
        ).isEqualTo(
            1l
        );
    }

    @Test
    public void getServiceDTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "serviceid", 1l
        ).request(
            APPLICATION_SERVICE_XML_TYPE
        ).get();

        ServiceReferenceDTO serviceReferenceDTO = response.readEntity(ServiceReferenceDTO.class);

        assertThat(
            serviceReferenceDTO.id
        ).isEqualTo(
            1l
        );
    }

    @Ignore("These are just tests for property coercion")
    @Test
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

    @Ignore("These are just tests for property coercion")
    @Test
    public void getService28DTOXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("service").path("{serviceid}");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "serviceid", 28l
        ).request(
            APPLICATION_SERVICE_XML_TYPE
        ).get();

        ServiceReferenceDTO serviceReferenceDTO = response.readEntity(ServiceReferenceDTO.class);

        assertThat(
            serviceReferenceDTO.id
        ).isEqualTo(
            28l
        );
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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.queryParam(
            "filter", "(service.id=25)"
        ).request(
            APPLICATION_SERVICES_JSON_TYPE
        ).get();

        ServicesDTO servicesDTO = response.readEntity(ServicesDTO.class);

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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.queryParam(
            "filter", "(service.id=25)"
        ).request(
            APPLICATION_SERVICES_XML_TYPE
        ).get();

        ServicesDTO servicesDTO = response.readEntity(ServicesDTO.class);

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

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.queryParam(
            "filter", "(objectClass=org.apache.aries.jax.rs.*)"
        ).request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        ServiceReferenceDTOs serviceReferenceDTOs = response.readEntity(ServiceReferenceDTOs.class);

        assertThat(
            serviceReferenceDTOs.services.get(0)
        ).hasFieldOrProperty(
            "bundle"
        ).hasFieldOrProperty(
            "id"
        ).hasFieldOrProperty(
            "properties"
        ).hasFieldOrProperty(
            "usingBundles"
        );

        Map<String, Object> properties = serviceReferenceDTOs.services.get(0).properties;

        assertThat(properties).contains(
            entry("objectClass", new String[] {"org.apache.aries.jax.rs.rest.management.internal.FrameworkResource"})
        ).contains(
            entry("osgi.jaxrs.application.select", "(osgi.jaxrs.name=RestManagementApplication)")
        );
    }

    @Test
    public void getServicesFilterDTOJSON_caseInsensitiveMatching() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("services").path("representations");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.queryParam(
            "filter", "(objectclass=org.apache.aries.jax.rs.*)"
        ).request(
            APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
        ).get();

        ServiceReferenceDTOs serviceReferenceDTOs = response.readEntity(ServiceReferenceDTOs.class);

        assertThat(
            serviceReferenceDTOs.services.get(0)
        ).hasFieldOrProperty(
            "bundle"
        ).hasFieldOrProperty(
            "id"
        ).hasFieldOrProperty(
            "properties"
        ).hasFieldOrProperty(
            "usingBundles"
        );

        Map<String, Object> properties = serviceReferenceDTOs.services.get(0).properties;

        assertThat(properties).contains(
            entry("objectClass", new String[] {"org.apache.aries.jax.rs.rest.management.internal.FrameworkResource"})
        ).contains(
            entry("osgi.jaxrs.application.select", "(osgi.jaxrs.name=RestManagementApplication)")
        );
    }

}
