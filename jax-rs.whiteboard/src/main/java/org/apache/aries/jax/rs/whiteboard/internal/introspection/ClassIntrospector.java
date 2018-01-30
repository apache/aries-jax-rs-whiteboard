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
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassIntrospector {

    private static final List<MediaType> _ALL_TYPES_LIST =
        Collections.singletonList(JAXRSUtils.ALL_TYPES);

    public static Collection<ResourceMethodInfoDTO> getResourceMethodInfos(
        Class<?> clazz, Bus bus) {

        ClassResourceInfo classResourceInfo =
            ResourceUtils.createClassResourceInfo(
                clazz, clazz, true, true, bus);

        Stream<ResourceMethodInfoDTO> convert = convert(
            new HashSet<>(), "/", null, _ALL_TYPES_LIST, _ALL_TYPES_LIST,
            Collections.emptySet(), true, classResourceInfo);

        return convert.collect(Collectors.toList());
    }

    private static Stream<ResourceMethodInfoDTO> convert(
        Set<ClassResourceInfo> visited,
        String parentPath, String defaultHttpMethod,
        List<MediaType> defaultConsumeTypes,
        List<MediaType> defaultProduceTypes,
        Set<String> defaultNameBindings, boolean recurse,
        ClassResourceInfo classResourceInfo) {

        visited.add(classResourceInfo);

        String path = parentPath + getPathSafe(classResourceInfo);

        List<MediaType> consumeMime = classResourceInfo.getConsumeMime();
        if (consumeMime.equals(_ALL_TYPES_LIST)) {
            consumeMime = defaultConsumeTypes;
        }

        List<MediaType> produceMime = classResourceInfo.getProduceMime();
        if (consumeMime.equals(_ALL_TYPES_LIST)) {
            produceMime = defaultProduceTypes;
        }

        Set<String> nameBindings = classResourceInfo.getNameBindings();
        if (nameBindings.isEmpty()) {
            nameBindings = defaultNameBindings;
        }

        MethodDispatcher methodDispatcher =
            classResourceInfo.getMethodDispatcher();

        Set<OperationResourceInfo> operationResourceInfos =
            methodDispatcher.getOperationResourceInfos();

        ArrayList<MediaType> consumeParam = new ArrayList<>(consumeMime);
        ArrayList<MediaType> produceParam = new ArrayList<>(produceMime);
        HashSet<String> nameBindingsParam = new HashSet<>(nameBindings);

        Stream<ResourceMethodInfoDTO> stream =
            operationResourceInfos.stream().
            flatMap(
                ori -> convert(
                    visited, path, defaultHttpMethod, consumeParam,
                    produceParam, nameBindingsParam, recurse, ori)
            ).collect(
                Collectors.toList()
            ).stream();

        visited.remove(classResourceInfo);

        return stream;
    }

    private static Stream<ResourceMethodInfoDTO> convert(
        Set<ClassResourceInfo> visited,
        String parentPath, String defaultHttpMethod,
        List<MediaType> defaultConsumeTypes,
        List<MediaType> defaultProduceTypes,
        Set<String> defaultNameBindings, boolean recurse,
        OperationResourceInfo operationResourceInfo) {

        List<MediaType> consumeTypes = operationResourceInfo.getConsumeTypes();
        if (consumeTypes == null) {
            consumeTypes = defaultConsumeTypes;
        }

        List<MediaType> produceTypes = operationResourceInfo.getProduceTypes();
        if (produceTypes == null) {
            produceTypes = defaultProduceTypes;
        }

        String httpMethod = operationResourceInfo.getHttpMethod();

        if (httpMethod == null) {
            httpMethod = defaultHttpMethod;
        }

        Set<String> nameBindings = operationResourceInfo.getNameBindings();
        if (nameBindings.isEmpty()) {
            nameBindings = defaultNameBindings;
        }

        String path = parentPath +
            operationResourceInfo.getURITemplate().getValue();

        if (operationResourceInfo.isSubResourceLocator()) {
            ClassResourceInfo classResourceInfo =
                operationResourceInfo.getClassResourceInfo();

            Class<?> returnType =
                operationResourceInfo.getAnnotatedMethod().getReturnType();

            ClassResourceInfo subResource = classResourceInfo.getSubResource(
                returnType, returnType);

            if (subResource != null) {
                if (recurse) {
                    return convert(visited,
                        path, httpMethod, consumeTypes, produceTypes,
                        nameBindings, !visited.contains(subResource),
                        subResource);
                }
                else {
                    return Stream.empty();
                }
            }
        }

        ResourceMethodInfoDTO resourceMethodInfoDTO =
            new ResourceMethodInfoDTO();

        resourceMethodInfoDTO.consumingMimeType = consumeTypes.stream().
            map(
                MediaType::toString
            ).toArray(
                String[]::new
            );

        resourceMethodInfoDTO.producingMimeType = produceTypes.stream().
            map(
                MediaType::toString
            ).toArray(
                String[]::new
            );

        resourceMethodInfoDTO.nameBindings = nameBindings.toArray(
            new String[0]);

        try {
            resourceMethodInfoDTO.path = Paths.get(path).normalize().toString();
        }
        catch (Exception e) {
            resourceMethodInfoDTO.path = "/";
        }
        resourceMethodInfoDTO.method = httpMethod;

        return Stream.of(resourceMethodInfoDTO);
    }

    private static String getPathSafe(ClassResourceInfo classResourceInfo) {
        Path path = classResourceInfo.getPath();
        if (path == null) {
            return "/";
        }

        return path.value();
    }

}
