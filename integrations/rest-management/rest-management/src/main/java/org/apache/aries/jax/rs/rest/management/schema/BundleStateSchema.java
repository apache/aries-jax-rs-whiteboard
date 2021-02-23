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

import org.osgi.dto.DTO;

@XmlRootElement(name = "bundleState")
public class BundleStateSchema extends DTO {

    public int state;

    public int options;

    public static BundleStateSchema build(int state, int options) {
        final BundleStateSchema bundleState = new BundleStateSchema();
        bundleState.state = state;
        bundleState.options = options;
        return bundleState;
    }

}
