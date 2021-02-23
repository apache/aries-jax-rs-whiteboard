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

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osgi.dto.DTO;

@XmlRootElement(name = "bundles")
@XmlAccessorType(FIELD)
public class BundleListSchema extends DTO {
    @XmlElement(name = "uri")
    public List<String> bundles;

    public static BundleListSchema build(List<String> bundles) {
        final BundleListSchema bundleListSchema = new BundleListSchema();
        bundleListSchema.bundles = bundles;
        return bundleListSchema;
    }

}
