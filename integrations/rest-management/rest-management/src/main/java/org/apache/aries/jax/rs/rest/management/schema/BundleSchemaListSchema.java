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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osgi.dto.DTO;

@XmlRootElement(name = "bundles")
public class BundleSchemaListSchema extends DTO {
    @XmlElement(name = "bundle")
    public List<BundleSchema> bundles;

    public static BundleSchemaListSchema build(List<BundleSchema> bundles) {
        final BundleSchemaListSchema bundlesSchema = new BundleSchemaListSchema();
        bundlesSchema.bundles = bundles;
        return bundlesSchema;
    }

}
