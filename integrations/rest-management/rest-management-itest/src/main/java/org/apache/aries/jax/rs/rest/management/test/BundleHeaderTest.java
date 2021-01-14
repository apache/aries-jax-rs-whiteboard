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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEHEADER_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEHEADER_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Dictionary;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.handler.RestManagementMessageBodyHandler;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit4.context.BundleContextRule;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundleHeaderTest extends TestUtil {

    @Rule
    public BundleContextRule bundleContextRule = new BundleContextRule();

    @InjectBundleContext
    BundleContext bundleContext;

    @Test
    public void getBundleHeaderJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("header");

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLEHEADER_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("Bnd-LastModified").isString()
        );
    }

    @Test
    public void getBundleHeaderXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("header");

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLEHEADER_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid(
        ).valueByXPath(
            "//bundleHeader/entry/@key"
        ).isEqualTo(
            "Bundle-Name"
        );
    }

    @Test
    public void getBundleHeaderDictionaryJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("header");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLEHEADER_JSON_TYPE
        ).get();

        Dictionary<String, String> bundleHeader = response.readEntity(
            new GenericType<Dictionary<String, String>>() {});

        assertThat(
            bundleHeader
        ).hasFieldOrPropertyWithValue(
            "Manifest-Version", "1.0"
        ).hasFieldOrPropertyWithValue(
            "Bundle-ManifestVersion", "2"
        );
    }

    @Test
    public void getBundleHeaderDictionaryXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("header");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLEHEADER_XML_TYPE
        ).get();

        Dictionary<String, String> bundleHeader = response.readEntity(
            new GenericType<Dictionary<String, String>>() {});

        assertThat(
            bundleHeader
        ).hasFieldOrPropertyWithValue(
            "Manifest-Version", "1.0"
        ).hasFieldOrPropertyWithValue(
            "Bundle-ManifestVersion", "2"
        );
    }

}
