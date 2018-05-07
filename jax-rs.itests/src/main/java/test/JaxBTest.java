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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import jaxb.types.Product;
import test.types.TestHelper;
import test.types.TestJAXBAddon;

public class JaxBTest extends TestHelper {

    @Test
    public void testJAXBEndPoint() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test");

        getRuntimeDTO();

        registerAddon(new TestJAXBAddon());

        Response response = webTarget.request(MediaType.APPLICATION_XML).get();

        String result = response.readEntity(String.class);

        assertTrue(
            "This should contain XML with <name>test</name>", result.contains("<name>test</name>"));
    }

    @Test
    public void testJAXBClientConversion() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test");

        getRuntimeDTO();

        registerAddon(new TestJAXBAddon());

        Product product = webTarget.request(MediaType.APPLICATION_XML).get(Product.class);

        assertEquals("Description: test", product.description);
        assertEquals(25, product.id);
        assertEquals("test", product.name);
        assertEquals(100, product.price);
    }

    @Test
    public void testJAXBClientPut() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("create");

        getRuntimeDTO();

        registerAddon(new TestJAXBAddon());

        Product p = new Product();
        p.description = "Description: BAR";
        p.id = 10023;
        p.name = "BAR";
        p.price = 100;

        Entity<Product> entity = Entity.entity(p, MediaType.APPLICATION_XML_TYPE);

        Response response = webTarget.request(MediaType.APPLICATION_XML).put(entity);

        assertEquals(200, response.getStatus());
        assertEquals(p.id + "", response.readEntity(String.class));
    }

}
