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

package test.types;


import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;

import java.util.Arrays;

public class ResourceMethodInfoDTOWrapper
    extends ResourceMethodInfoDTO {

    public ResourceMethodInfoDTOWrapper(
        ResourceMethodInfoDTO resourceMethodInfoDTO) {

        path = resourceMethodInfoDTO.path;
        method = resourceMethodInfoDTO.method;
        consumingMimeType = resourceMethodInfoDTO.consumingMimeType;
        producingMimeType = resourceMethodInfoDTO.producingMimeType;
        nameBindings = resourceMethodInfoDTO.nameBindings;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceMethodInfoDTO)) return false;

        ResourceMethodInfoDTO that = (ResourceMethodInfoDTO) o;

        return
            path.equals(that.path) &&
            method.equals(that.method) &&
            Arrays.asList(consumingMimeType).equals(
                Arrays.asList(that.consumingMimeType)) &&
            Arrays.asList(producingMimeType).equals(
                Arrays.asList(that.producingMimeType)) &&
            Arrays.asList(nameBindings).equals(
                Arrays.asList(that.nameBindings));
    }

}
