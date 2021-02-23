package org.apache.aries.jax.rs.rest.management.feature;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class RestManagementFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new JacksonJsonProvider());

        return true;
    }

}
