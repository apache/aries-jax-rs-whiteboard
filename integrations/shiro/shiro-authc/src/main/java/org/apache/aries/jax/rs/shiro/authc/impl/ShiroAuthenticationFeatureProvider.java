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
package org.apache.aries.jax.rs.shiro.authc.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Feature;

import org.apache.shiro.realm.Realm;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ShiroAuthenticationFeatureProvider implements PrototypeServiceFactory<Feature> {

    private final List<Realm> realms;
    
    private final Set<ShiroAuthenticationFeature> features = Collections.synchronizedSet(new HashSet<>());
    
    public ShiroAuthenticationFeatureProvider(List<Realm> realms) {
        this.realms = realms;
    }
    
    @Override
    public ShiroAuthenticationFeature getService(Bundle bundle, ServiceRegistration<Feature> registration) {
        ShiroAuthenticationFeature authenticationFeature = new ShiroAuthenticationFeature(realms);
        features.add(authenticationFeature);
        return authenticationFeature;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Feature> registration, Feature service) {
        if(features.remove(service)) {
            ((ShiroAuthenticationFeature) service).close();
        }
    }

    public void close() {
        features.forEach(ShiroAuthenticationFeature::close);
    }

}
