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

package org.apache.aries.jax.rs.whiteboard.internal.cxf;

import org.apache.aries.jax.rs.whiteboard.internal.utils.ServiceReferenceResourceProvider;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

import java.util.Comparator;

public class ComparableResourceComparator
    implements ResourceComparator {

    static {
        comparator = Comparator.comparing(
            srrp -> srrp.getImmutableServiceReference());
    }

    @Override
    public int compare(
        ClassResourceInfo cri1, ClassResourceInfo cri2, Message message) {

        ResourceProvider rp1 = cri1.getResourceProvider();
        ResourceProvider rp2 = cri2.getResourceProvider();

        if (rp1 instanceof ServiceReferenceResourceProvider &&
            rp2 instanceof ServiceReferenceResourceProvider) {

            return comparator.compare(
                (ServiceReferenceResourceProvider)rp2,
                (ServiceReferenceResourceProvider)rp1);
        }

        if (rp1 instanceof ServiceReferenceResourceProvider) {
            return -1;
        }

        if (rp2 instanceof ServiceReferenceResourceProvider) {
            return 1;
        }

        return 0;
    }

    @Override
    public int compare(
        OperationResourceInfo oper1, OperationResourceInfo oper2,
        Message message) {

        return 0;
    }
    private static Comparator<ServiceReferenceResourceProvider> comparator;

}
