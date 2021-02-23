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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.schema.BundleStateSchema;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class FrameworkStateTest extends TestUtil {

    @Test
    public void getFrameworkStateJSON() {
        WebTarget target = createDefaultTarget().path("framework").path("state");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("state").isNumber().isEqualByComparingTo("32"),
            j -> j.node("options").isNumber().isEqualByComparingTo("2")
        );
    }

    @Test
    public void getFrameworkStateJSON_DOT() {
        WebTarget target = createDefaultTarget().path("framework").path("state.json");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("state").isNumber().isEqualByComparingTo("32"),
            j -> j.node("options").isNumber().isEqualByComparingTo("2")
        );
    }

    @Test
    public void getFrameworkStateXML() {
        WebTarget target = createDefaultTarget().path("framework").path("state");

        Response response = target.request(
            APPLICATION_BUNDLESTATE_XML_TYPE).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//bundleState/state"
        ).contains("32");
   }

    @Test
    public void getFrameworkStateXML_DOT() {
        WebTarget target = createDefaultTarget().path("framework").path("state.xml");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//bundleState/state"
        ).contains("32");
   }

    @Test
    public void getFrameworkStateDTO() {
        WebTarget target = createDefaultTarget().path("framework").path("state");

        Response response = target.request().get();

        BundleStateSchema bundleStateDTO = response.readEntity(BundleStateSchema.class);

        assertThat(
            bundleStateDTO.state
        ).isEqualTo(
            Bundle.ACTIVE
        );
    }

}
