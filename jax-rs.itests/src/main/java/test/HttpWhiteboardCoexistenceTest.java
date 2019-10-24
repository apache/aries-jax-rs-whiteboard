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

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import test.types.TestAddon;
import test.types.TestHelper;

public class HttpWhiteboardCoexistenceTest extends TestHelper {

    @Test
    public void testEndPointWhichRegistersStaticResource() throws Exception {
        WebTarget endpointWebTarget = createDefaultTarget().path("test");
        WebTarget resourceWebTarget = createDefaultTarget().path("resources/static-resource.txt");
        WebTarget nestedResourceWebTarget = createDefaultTarget().path("resources/nested/nested-static-resource.txt");

        getRuntimeDTO();

        registerAddon(new TestAddon(), 
                HTTP_WHITEBOARD_RESOURCE_PATTERN, "/resources/*",
                HTTP_WHITEBOARD_RESOURCE_PREFIX, "/");

        Response response = endpointWebTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        response = resourceWebTarget.request().get();
        
        assertEquals(
                "This should be the static resource, but was " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase(), 
                "A Static Resource", response.readEntity(String.class));

        response = nestedResourceWebTarget.request().get();
        
        assertEquals(
                "This should be the nested static resource, but was " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase(),
                "A Nested Static Resource", response.readEntity(String.class));
        
        // Restart the Http Service, then check that the test still works!
        restartHttpServiceWhiteboard();
        
        endpointWebTarget = createDefaultTarget().path("test");
        resourceWebTarget = createDefaultTarget().path("resources/static-resource.txt");
        nestedResourceWebTarget = createDefaultTarget().path("resources/nested/nested-static-resource.txt");
        
        response = endpointWebTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        response = resourceWebTarget.request().get();
        
        assertEquals(
                "This should be the static resource, but was " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase(),
                "A Static Resource", response.readEntity(String.class));

        response = nestedResourceWebTarget.request().get();
        
        assertEquals(
                "This should be the nested static resource, but was " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase(),
                "A Nested Static Resource", response.readEntity(String.class));
        
    }

    private void restartHttpServiceWhiteboard() throws Exception {
        BundleWiring runtimeWiring = _runtimeServiceReference.getBundle().adapt(BundleWiring.class);
        
        Bundle httpService = runtimeWiring.getRequiredWires("osgi.implementation").stream()
            .filter(bw -> "osgi.http".equals(bw.getCapability().getAttributes().get("osgi.implementation")))
            .map(bw -> bw.getProvider().getBundle())
            .findFirst().get();
        
        try {
            httpService.stop();
        } finally {
            httpService.start();
        }
        
        _runtime = _runtimeTracker.waitForService(5000);
        _runtimeServiceReference = _runtimeTracker.getServiceReference();
    }

    @Test
    public void testEndPointWhichRegistersStaticResourceWithFolder() throws Exception {
        WebTarget endpointWebTarget = createDefaultTarget().path("test");
        WebTarget resourceWebTarget = createDefaultTarget().path("resources/static-resource.txt");
        WebTarget nestedResourceWebTarget = createDefaultTarget().path("resources/nested-static-resource.txt");
        
        getRuntimeDTO();
        
        registerAddon(new TestAddon(), 
                HTTP_WHITEBOARD_RESOURCE_PATTERN, "/resources/*",
                HTTP_WHITEBOARD_RESOURCE_PREFIX, "nested");
        
        Response response = endpointWebTarget.request().get();
        
        assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));
        
        response = resourceWebTarget.request().get();
        
        assertEquals(
                "The static resource should not be found", NOT_FOUND.getStatusCode(),
                response.getStatus());
        
        response = nestedResourceWebTarget.request().get();
        
        assertEquals(
                "This should be the nested static resource, but was " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase(), 
                "A Nested Static Resource", response.readEntity(String.class));
        
        // Restart the Http Service, then check that the test still works!
        restartHttpServiceWhiteboard();
        
        endpointWebTarget = createDefaultTarget().path("test");
        resourceWebTarget = createDefaultTarget().path("resources/static-resource.txt");
        nestedResourceWebTarget = createDefaultTarget().path("resources/nested-static-resource.txt");
        
        response = endpointWebTarget.request().get();
        
        assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));
        
        response = resourceWebTarget.request().get();
        
        assertEquals(
                "The static resource should not be found", NOT_FOUND.getStatusCode(),
                response.getStatus());
        
        response = nestedResourceWebTarget.request().get();
        
        assertEquals(
                "This should be the nested static resource, but was " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase(),
                "A Nested Static Resource", response.readEntity(String.class));
    }

}