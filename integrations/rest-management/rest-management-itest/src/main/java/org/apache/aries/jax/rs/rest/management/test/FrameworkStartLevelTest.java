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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_FRAMEWORKSTARTLEVEL_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.handler.RestManagementMessageBodyHandler;
import org.junit.Test;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class FrameworkStartLevelTest extends TestUtil {

    @Test
    public void getFrameworkStartLevelJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("startLevel").isNumber().isBetween(new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE)),
            j -> j.node("initialBundleStartLevel").isNumber().isBetween(new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE))
        );
    }

    @Test
    public void getFrameworkStartLevelJSON_DOT() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel.json");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("startLevel").isNumber().isBetween(new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE)),
            j -> j.node("initialBundleStartLevel").isNumber().isBetween(new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE))
        );
    }

    @Test
    public void getFrameworkStartLevelJSONExplicit() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        Response response = target.request(
            APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("startLevel").isNumber().isBetween(new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE)),
            j -> j.node("initialBundleStartLevel").isNumber().isBetween(new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE))
        );
    }

    @Test
    public void getFrameworkStartLevelXML() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        Response response = target.request(
            APPLICATION_FRAMEWORKSTARTLEVEL_XML_TYPE).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid(
        ).valueByXPath(
            "//frameworkStartLevel/startLevel"
        ).asInt(
        ).isBetween(
            0, Integer.MAX_VALUE
        );
    }

    @Test
    public void getFrameworkStartLevelXML_DOT() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel.xml");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid(
        ).valueByXPath(
            "//frameworkStartLevel/startLevel"
        ).asInt(
        ).isBetween(
            0, Integer.MAX_VALUE
        );
    }

    @Test
    public void getFrameworkStartLevelDTO() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.request().get();

        FrameworkStartLevelDTO frameworkStartLevelDTO =
            response.readEntity(FrameworkStartLevelDTO.class);

        assertThat(
            frameworkStartLevelDTO.startLevel
        ).isGreaterThan(
            0
        );
        assertThat(
            frameworkStartLevelDTO.initialBundleStartLevel
        ).isGreaterThan(
            0
        );
    }

    @Test
    public void putFrameworkStartLevelBadValues() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        target.register(RestManagementMessageBodyHandler.class);

        FrameworkStartLevelDTO frameworkStartLevelDTO =
            new FrameworkStartLevelDTO();

        Response response = target.request().put(
            Entity.entity(
                frameworkStartLevelDTO, APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE));

        assertThat(
            response.getStatus()
        ).isEqualTo(
            400
        );
    }

    @Test
    public void putFrameworkStartLevel() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        target.register(RestManagementMessageBodyHandler.class);

        FrameworkStartLevelDTO frameworkStartLevelDTO =
            new FrameworkStartLevelDTO();

        frameworkStartLevelDTO.startLevel = 2;
        frameworkStartLevelDTO.initialBundleStartLevel = 1;

        try {
            Response response = target.request().put(
                Entity.entity(
                    frameworkStartLevelDTO, APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE));

            assertThat(
                response.getStatus()
            ).isEqualTo(
                204
            );
        }
        finally {
            frameworkStartLevelDTO.startLevel = 1;

            Response response = target.request().put(
                Entity.entity(
                    frameworkStartLevelDTO, APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE));

            assertThat(
                response.getStatus()
            ).isEqualTo(
                204
            );
        }
    }

    @Test
    public void putFrameworkStartLevel_2() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("startlevel");

        target.register(RestManagementMessageBodyHandler.class);

        FrameworkStartLevelDTO frameworkStartLevelDTO =
            new FrameworkStartLevelDTO();

        frameworkStartLevelDTO.startLevel = 1;
        frameworkStartLevelDTO.initialBundleStartLevel = 2;

        try {
            Response response = target.request().put(
                Entity.entity(
                    frameworkStartLevelDTO, APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE));

            assertThat(
                response.getStatus()
            ).isEqualTo(
                204
            );
        }
        finally {
            frameworkStartLevelDTO.initialBundleStartLevel = 1;

            Response response = target.request().put(
                Entity.entity(
                    frameworkStartLevelDTO, APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE));

            assertThat(
                response.getStatus()
            ).isEqualTo(
                204
            );
        }
    }

}
