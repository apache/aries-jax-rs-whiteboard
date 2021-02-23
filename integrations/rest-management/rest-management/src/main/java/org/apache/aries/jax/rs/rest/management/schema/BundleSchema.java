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

package org.apache.aries.jax.rs.rest.management.schema;

import javax.xml.bind.annotation.XmlRootElement;

import org.osgi.framework.dto.BundleDTO;

@XmlRootElement(name = "bundle")
public class BundleSchema extends BundleDTO {

    public String location;

    public static BundleSchema build(
        long id, long lastModified, int state, String symbolicName, String version, String location) {
        final BundleSchema bundleSchema = new BundleSchema();
        bundleSchema.id = id;
        bundleSchema.lastModified = lastModified;
        bundleSchema.state = state;
        bundleSchema.symbolicName = symbolicName;
        bundleSchema.version = version;
        bundleSchema.location = location;
        return bundleSchema;
    }

    public static BundleSchema build(BundleDTO bundleDTO) {
        final BundleSchema bundleSchema = new BundleSchema();
        bundleSchema.id = bundleDTO.id;
        bundleSchema.lastModified = bundleDTO.lastModified;
        bundleSchema.state = bundleDTO.state;
        bundleSchema.symbolicName = bundleDTO.symbolicName;
        bundleSchema.version = bundleDTO.version;
        return bundleSchema;
    }

}
