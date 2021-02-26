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

package org.apache.aries.jax.rs.rest.management.internal;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.ws.rs.core.Application;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.jax.rs.rest.management.internal.jaxb.ServiceSchemaContextResolver;
import org.osgi.framework.BundleContext;
import org.osgi.service.rest.RestApiExtension;

public class RestManagementApplication extends Application {

    private final BundleContext bundleContext;
    private final Set<CachingServiceReference<RestApiExtension>> extensions =
        new ConcurrentSkipListSet<>(Comparator.naturalOrder());

    public RestManagementApplication(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();

        singletons.add(new ServiceSchemaContextResolver());

        singletons.add(new ExtensionResource(bundleContext, extensions));
        singletons.add(new FrameworkBundleHeaderResource(bundleContext));
        singletons.add(new FrameworkBundleResource(bundleContext));
        singletons.add(new FrameworkBundlesRepresentationsResource(bundleContext));
        singletons.add(new FrameworkBundlesResource(bundleContext));
        singletons.add(new FrameworkBundleStartLevelResource(bundleContext));
        singletons.add(new FrameworkBundleStateResource(bundleContext));
        singletons.add(new FrameworkResource(bundleContext));
        singletons.add(new FrameworkServiceResource(bundleContext));
        singletons.add(new FrameworkServicesRepresentationsResource(bundleContext));
        singletons.add(new FrameworkServicesResource(bundleContext));
        singletons.add(new FrameworkStartLevelResource(bundleContext));
        singletons.add(new FrameworkStateResource(bundleContext));
        return singletons;
    }

    public void addExtension(CachingServiceReference<RestApiExtension> extension) {
        extensions.add(extension);
    }

    public void removeExtension(CachingServiceReference<RestApiExtension> extension) {
        extensions.remove(extension);
    }

}
