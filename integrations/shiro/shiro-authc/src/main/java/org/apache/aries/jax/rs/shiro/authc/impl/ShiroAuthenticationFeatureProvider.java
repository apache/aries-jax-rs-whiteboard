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
