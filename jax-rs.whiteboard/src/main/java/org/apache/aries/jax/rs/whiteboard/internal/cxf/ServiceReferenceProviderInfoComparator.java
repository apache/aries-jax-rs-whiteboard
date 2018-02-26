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

import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ProviderFactory.ProviderInfoClassComparator;
import org.osgi.framework.ServiceReference;

import java.util.Comparator;

public class ServiceReferenceProviderInfoComparator implements
    Comparator<ProviderInfo<?>> {

    public ServiceReferenceProviderInfoComparator(
        ProviderInfoClassComparator providerInfoClassComparator) {

        _providerInfoClassComparator = providerInfoClassComparator;
    }

    @Override
    public int compare(ProviderInfo<?> pi1, ProviderInfo<?> pi2) {
        if (pi1 instanceof ServiceReferenceFilterProviderInfo<?>) {
            if (pi2 instanceof ServiceReferenceFilterProviderInfo<?>) {
                CachingServiceReference serviceReference1 =
                    ((ServiceReferenceFilterProviderInfo) pi1).
                        getServiceReference();
                CachingServiceReference serviceReference2 =
                    ((ServiceReferenceFilterProviderInfo) pi2).
                        getServiceReference();

                return serviceReference1.compareTo(serviceReference2);
            }
            else {
                return -1;
            }
        }
        else {
            if (pi2 instanceof ServiceReferenceFilterProviderInfo<?>) {
                return 1;
            }
        }

        return _providerInfoClassComparator.compare(pi1, pi2);
    }

    private final ProviderInfoClassComparator
        _providerInfoClassComparator;
}
