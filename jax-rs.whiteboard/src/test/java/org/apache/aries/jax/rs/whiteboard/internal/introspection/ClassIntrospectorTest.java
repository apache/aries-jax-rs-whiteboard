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

package org.apache.aries.jax.rs.whiteboard.internal.introspection;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import test.types.PlainResource;
import test.types.PlainResourceSeveralOperations;
import test.types.PlainResourceSeveralOperationsCommonPath;
import test.types.PlainResourceSeveralOperationsDifferentPath;
import test.types.PlainResourceSeveralOperationsWithNameBinding;
import test.types.ResourceMethodInfoDTOWrapper;
import test.types.ResourceWithSubResource;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClassIntrospectorTest {

    private static final String[] ALL_TYPES = {MediaType.WILDCARD};

    @Test
    public void testPlainResource() {
        Bus bus = BusFactory.getDefaultBus(true);

        ResourceMethodInfoDTO[] resourceMethodInfoDTOS =
            ClassIntrospector.getResourceMethodInfos(PlainResource.class, bus);

        assertEquals(1, resourceMethodInfoDTOS.length);

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            resourceMethodInfoDTOS[0];

        assertEquals(HttpMethod.GET, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);
    }

    @Test
    public void testPlainResourceWithNameBinding() {
        Bus bus = BusFactory.getDefaultBus(true);

        ResourceMethodInfoDTO[] resourceMethodInfoDTOS =
            ClassIntrospector.getResourceMethodInfos(
                PlainResourceSeveralOperationsWithNameBinding.class, bus);

        assertEquals(2, resourceMethodInfoDTOS.length);

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            resourceMethodInfoDTOS[0];

        assertEquals(HttpMethod.GET, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/", resourceMethodInfoDTO.path);
        assertArrayEquals(
            new String[]{"test.types.Bound"},
            resourceMethodInfoDTO.nameBindings);

        resourceMethodInfoDTO = resourceMethodInfoDTOS[1];

        assertEquals(HttpMethod.POST, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/", resourceMethodInfoDTO.path);
        assertArrayEquals(
            new String[]{"test.types.Bound"},
            resourceMethodInfoDTO.nameBindings);
    }


    @Test
    public void testPlainResourceSeveralOperations() {
        Bus bus = BusFactory.getDefaultBus(true);

        ResourceMethodInfoDTO[] resourceMethodInfoDTOS =
            ClassIntrospector.getResourceMethodInfos(
                PlainResourceSeveralOperations.class, bus);

        assertEquals(2, resourceMethodInfoDTOS.length);

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            resourceMethodInfoDTOS[0];

        assertEquals(HttpMethod.GET, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);

        resourceMethodInfoDTO = resourceMethodInfoDTOS[1];

        assertEquals(HttpMethod.POST, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);
    }

    @Test
    public void testPlainResourceSeveralOperationsWithCommonPath() {
        Bus bus = BusFactory.getDefaultBus(true);

        ResourceMethodInfoDTO[] resourceMethodInfoDTOS =
            ClassIntrospector.getResourceMethodInfos(
                PlainResourceSeveralOperationsCommonPath.class, bus);

        assertEquals(2, resourceMethodInfoDTOS.length);

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            resourceMethodInfoDTOS[0];

        assertEquals(HttpMethod.GET, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/common", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);

        resourceMethodInfoDTO = resourceMethodInfoDTOS[1];

        assertEquals(HttpMethod.POST, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/common", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);
    }

    @Test
    public void testPlainResourceSeveralOperationsWithDifferentPath() {
        Bus bus = BusFactory.getDefaultBus(true);

        ResourceMethodInfoDTO[] resourceMethodInfoDTOS =
            ClassIntrospector.getResourceMethodInfos(
                PlainResourceSeveralOperationsDifferentPath.class, bus);

        assertEquals(2, resourceMethodInfoDTOS.length);

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            resourceMethodInfoDTOS[0];

        assertEquals(HttpMethod.GET, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/common", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);

        resourceMethodInfoDTO = resourceMethodInfoDTOS[1];

        assertEquals(HttpMethod.POST, resourceMethodInfoDTO.method);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.consumingMimeType);
        assertArrayEquals(
            ALL_TYPES, resourceMethodInfoDTO.producingMimeType);
        assertEquals("/common/different", resourceMethodInfoDTO.path);
        assertArrayEquals(new String[]{}, resourceMethodInfoDTO.nameBindings);
    }

    @Test
    public void testResourceWithSubresource() {
        Bus bus = BusFactory.getDefaultBus(true);

        ResourceMethodInfoDTO[] resourceMethodInfoDTOS =
            ClassIntrospector.getResourceMethodInfos(
                ResourceWithSubResource.class, bus);

        assertEquals(5, resourceMethodInfoDTOS.length);

        List<ResourceMethodInfoDTOWrapper> wrappers = Arrays.stream(
            resourceMethodInfoDTOS
        ).map(
            ResourceMethodInfoDTOWrapper::new
        ).collect(
            Collectors.toList()
        );

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            new ResourceMethodInfoDTO();

        resourceMethodInfoDTO.method = HttpMethod.GET;
        resourceMethodInfoDTO.consumingMimeType = ALL_TYPES;
        resourceMethodInfoDTO.producingMimeType =
            new String[]{MediaType.APPLICATION_XML};
        resourceMethodInfoDTO.path = "/resource";
        resourceMethodInfoDTO.nameBindings = new String[]{};

        assertTrue(wrappers.remove(new ResourceMethodInfoDTOWrapper(resourceMethodInfoDTO)));

        resourceMethodInfoDTO = new ResourceMethodInfoDTO();

        resourceMethodInfoDTO.method = HttpMethod.GET;
        resourceMethodInfoDTO.consumingMimeType =
            new String[]{MediaType.APPLICATION_JSON};
        resourceMethodInfoDTO.producingMimeType =
            new String[]{MediaType.APPLICATION_JSON};
        resourceMethodInfoDTO.path = "/resource/subresource";
        resourceMethodInfoDTO.nameBindings = new String[]{};

        assertTrue(wrappers.remove(new ResourceMethodInfoDTOWrapper(resourceMethodInfoDTO)));

        resourceMethodInfoDTO = new ResourceMethodInfoDTO();

        resourceMethodInfoDTO.method = HttpMethod.POST;
        resourceMethodInfoDTO.consumingMimeType =
            new String[]{MediaType.APPLICATION_XML};
        resourceMethodInfoDTO.producingMimeType =
            new String[]{
                MediaType.TEXT_PLAIN,
                MediaType.APPLICATION_JSON
            };
        resourceMethodInfoDTO.path = "/resource/subresource";
        resourceMethodInfoDTO.nameBindings = new String[]{};

        assertTrue(wrappers.remove(new ResourceMethodInfoDTOWrapper(resourceMethodInfoDTO)));

        resourceMethodInfoDTO = new ResourceMethodInfoDTO();

        resourceMethodInfoDTO.method = HttpMethod.GET;
        resourceMethodInfoDTO.consumingMimeType =
            new String[]{MediaType.APPLICATION_JSON};
        resourceMethodInfoDTO.producingMimeType =
            new String[]{MediaType.APPLICATION_JSON};
        resourceMethodInfoDTO.path = "/resource/subresource/{path}";
        resourceMethodInfoDTO.nameBindings = new String[]{};

        assertTrue(wrappers.remove(new ResourceMethodInfoDTOWrapper(resourceMethodInfoDTO)));

        resourceMethodInfoDTO = new ResourceMethodInfoDTO();

        resourceMethodInfoDTO.method = HttpMethod.POST;
        resourceMethodInfoDTO.consumingMimeType =
            new String[]{MediaType.APPLICATION_XML};
        resourceMethodInfoDTO.producingMimeType = new String[]{
            MediaType.TEXT_PLAIN,
            MediaType.APPLICATION_JSON
        };
        resourceMethodInfoDTO.path = "/resource/subresource/{path}";
        resourceMethodInfoDTO.nameBindings = new String[]{};

        assertTrue(wrappers.remove(new ResourceMethodInfoDTOWrapper(resourceMethodInfoDTO)));

        assertTrue(wrappers.isEmpty());
    }

}
