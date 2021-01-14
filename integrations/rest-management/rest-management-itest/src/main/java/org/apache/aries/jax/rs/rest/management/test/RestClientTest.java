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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.service.rest.client.RestClient;
import org.osgi.service.rest.client.RestClientFactory;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit4.context.BundleContextRule;

public class RestClientTest extends TestUtil {

    @Rule
    public BundleContextRule bundleContextRule = new BundleContextRule();

    @InjectBundleContext
    BundleContext bundleContext;

    @InjectService
    public RestClientFactory restClientFactory;

    @Test
    public void getFrameworkStartLevel() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        FrameworkStartLevelDTO frameworkStartLevel =
            restClient.getFrameworkStartLevel();

        assertThat(
            frameworkStartLevel.startLevel
        ).isGreaterThan(0);
        assertThat(
            frameworkStartLevel.initialBundleStartLevel
        ).isGreaterThan(0);
    }

    @Test
    public void setFrameworkStartLevel_UnsetValues() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        FrameworkStartLevelDTO frameworkStartLevel = new FrameworkStartLevelDTO();

        assertThatThrownBy(
            () -> restClient.setFrameworkStartLevel(frameworkStartLevel)
        );
    }

    @Test
    public void setFrameworkStartLevel_CorrectValues() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        FrameworkStartLevelDTO frameworkStartLevel = new FrameworkStartLevelDTO();

        frameworkStartLevel.initialBundleStartLevel = 2;
        frameworkStartLevel.startLevel = 1;

        restClient.setFrameworkStartLevel(frameworkStartLevel);
    }

    @Test
    public void getBundlePaths() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        Collection<String> bundlePaths = restClient.getBundlePaths();

        assertThat(
            bundlePaths.iterator().next()
        ).contains("framework/bundle/0");
    }

    @Test
    public void getBundleById() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        BundleDTO bundleDTO = restClient.getBundle(1l);

        assertThat(bundleDTO).hasFieldOrPropertyWithValue("id", 1l);
    }

    @Test
    public void getBundleByInvalidId() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        assertThatThrownBy(() ->
            restClient.getBundle(9999999l)
        );
    }

    @Test
    public void getBundleByPath() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        BundleDTO bundleDTO = restClient.getBundle("/framework/bundle/0");

        assertThat(
            bundleDTO
        ).hasFieldOrPropertyWithValue(
            "id", 0l
        ).hasFieldOrProperty(
            "lastModified"
        ).hasFieldOrPropertyWithValue(
            "state", 32
        ).hasFieldOrPropertyWithValue(
            "symbolicName", "org.eclipse.osgi"
        ).hasFieldOrProperty(
            "version"
        );
    }

    @Test
    public void installBundleByLocation() throws Exception {
        try (HttpServer server = new HttpServer(
                BundleTest.class.getClassLoader().getResource("minor-change-1.0.1.jar"),
                "application/zip")) {

            BundleDTO bundleDTO = null;

            try {
                RestClient restClient = restClientFactory.createRestClient(
                    runtimeURI());

                bundleDTO = restClient.installBundle(
                    String.format(
                        "http://localhost:%d/minor-change-1.0.1.jar",
                        server.getPort()
                    )
                );

                assertThat(
                    bundleDTO.symbolicName
                ).isEqualTo(
                    "minor-and-removed-change"
                );
            }
            finally {
                if (bundleDTO != null) {
                    bundleContext.getBundle(bundleDTO.id).uninstall();
                }
            }
        }
    }

    @Test
    public void installBundleByBadLocation() throws Exception {
        RestClient restClient = restClientFactory.createRestClient(
            runtimeURI());

        assertThatThrownBy(() -> {
            restClient.installBundle(
                "http://localhost:%d/minor-change-1.0.1.jar"
            );
        }).isInstanceOf(
            Exception.class
        ).hasMessageContaining(
            "Bad Request"
        );
    }

}
