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

package test;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;

import org.osgi.framework.ServiceRegistration;
import test.types.TestHelper;
import test.types.TestOpenApiResource;

import javax.ws.rs.client.WebTarget;

import java.util.Hashtable;

import static org.junit.Assert.assertTrue;

public class OpenApiTest extends TestHelper {

    @Test
    public void testOpenApiEndpoint() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.info(
            new Info()
                .title("My Service")
                .description("Service REST API")
                .contact(
                    new Contact()
                        .email("oschweitzer@me.com"))
        );

        ServiceRegistration<OpenAPI> serviceRegistration =
            bundleContext.registerService(
                OpenAPI.class, openAPI, new Hashtable<>());

        try {
            WebTarget webTarget = createDefaultTarget().
                path("openapi.json");

            registerAddon(new TestOpenApiResource());

            String response = webTarget.request().get(String.class);

            assertTrue(response.contains("operation"));
        }
        finally {
            serviceRegistration.unregister();
        }
    }

}
