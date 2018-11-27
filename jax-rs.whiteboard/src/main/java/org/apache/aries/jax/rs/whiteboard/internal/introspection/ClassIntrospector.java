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
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassIntrospector {

    public static Collection<ResourceMethodInfoDTO> getResourceMethodInfos(
        Class<?> clazz, Bus bus) {

        ClassResourceInfo classResourceInfo =
            ResourceUtils.createClassResourceInfo(
                clazz, clazz, true, true, bus);

        Stream<ResourceMethodInfoDTO> convert = convert(
            new HashSet<>(), "/", null, null, null, null,
            true, classResourceInfo);

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

        List<MediaType> consumeMime = getConsumesInfo(
            classResourceInfo.getResourceClass());

        if (consumeMime == null) {
            consumeMime = defaultConsumeTypes;
        }

        List<MediaType> produceMime = getProducesInfo(
            classResourceInfo.getResourceClass());

        if (produceMime == null) {
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

        List<MediaType> consumeParam = consumeMime == null ? null : new ArrayList<>(consumeMime);
        List<MediaType> produceParam = produceMime == null ? null : new ArrayList<>(produceMime);
        HashSet<String> nameBindingsParam = nameBindings == null ? null : new HashSet<>(nameBindings);

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

        List<MediaType> consumeTypes = getConsumesInfo(
            operationResourceInfo.getAnnotatedMethod());

        if (consumeTypes == null) {
            consumeTypes = defaultConsumeTypes;
        }

        List<MediaType> produceTypes = getProducesInfo(
            operationResourceInfo.getAnnotatedMethod());

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

        resourceMethodInfoDTO.consumingMimeType = consumeTypes == null ? null :
            consumeTypes.stream().
            map(
                MediaType::toString
            ).toArray(
                String[]::new
            );

        resourceMethodInfoDTO.producingMimeType = produceTypes == null ? null :
            produceTypes.stream().
            map(
                MediaType::toString
            ).toArray(
                String[]::new
            );

        resourceMethodInfoDTO.nameBindings = nameBindings == null ? null :
            nameBindings.toArray(new String[0]);

        resourceMethodInfoDTO.path = normalize(path);

        resourceMethodInfoDTO.method = httpMethod;

        return Stream.of(resourceMethodInfoDTO);
    }

    private static List<MediaType> getConsumesInfo(Method method) {
        Consumes consumes = AnnotationUtils.getMethodAnnotation(
            method, Consumes.class);

        return consumes == null ? null : JAXRSUtils.getMediaTypes(consumes.value());
    }

    private static List<MediaType> getConsumesInfo(Class<?> clazz) {
        Consumes consumes = AnnotationUtils.getClassAnnotation(
            clazz, Consumes.class);

        return consumes == null ? null : JAXRSUtils.getMediaTypes(consumes.value());
    }

    private static String getPathSafe(ClassResourceInfo classResourceInfo) {
        Path path = classResourceInfo.getPath();
        if (path == null) {
            return "/";
        }

        return path.value();
    }

    private static List<MediaType> getProducesInfo(Method method) {
        Produces produces = AnnotationUtils.getMethodAnnotation(
            method, Produces.class);

        return produces == null ? null : JAXRSUtils.getMediaTypes(produces.value());
    }

    private static List<MediaType> getProducesInfo(Class<?> clazz) {
        Produces produces = AnnotationUtils.getClassAnnotation(
            clazz, Produces.class);

        return produces == null ? null : JAXRSUtils.getMediaTypes(produces.value());
    }

    private static String normalize(String path) {
        return "/" + Arrays.stream(path.split("/")).
            filter(s -> !s.isEmpty()).
            collect(Collectors.joining("/"));
    }

}
