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

package org.apache.aries.jax.rs.whiteboard.internal;

import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET;

import java.util.function.Predicate;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetFilter<T> implements Predicate<CachingServiceReference<T>>  {

    public TargetFilter(ServiceReference<?> serviceRuntimeReference) {
        _serviceRuntimeReference = serviceRuntimeReference;
    }

    @Override
    public boolean test(CachingServiceReference<T> ref) {
        String target = (String)ref.getProperty(JAX_RS_WHITEBOARD_TARGET);

        if (target == null) {
            return true;
        }

        Filter filter;

        try {
            filter = FrameworkUtil.createFilter(target);
        }
        catch (InvalidSyntaxException ise) {
            if (_log.isErrorEnabled()) {
                _log.error("Invalid '{}' filter syntax in {}", JAX_RS_WHITEBOARD_TARGET, ref);
            }

            return false;
        }

        return filter.match(_serviceRuntimeReference);
    }

    private static final Logger _log = LoggerFactory.getLogger(TargetFilter.class);

    private final ServiceReference<?> _serviceRuntimeReference;

}