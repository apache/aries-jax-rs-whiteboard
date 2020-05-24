package org.apache.aries.jax.rs.openapi;

import java.util.Map;
import java.util.Set;

import org.apache.aries.jax.rs.whiteboard.ApplicationClasses;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;

public class JaxrsWhiteboardScanner implements OpenApiScanner {

    private final ApplicationClasses applicationClasses;
    public JaxrsWhiteboardScanner(ApplicationClasses applicationClasses) {
        this.applicationClasses = applicationClasses;
    }

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
    }

    @Override
    public Set<Class<?>> classes() {
        return applicationClasses.classes();
    }

    @Override
    public Map<String, Object> resources() {
        return null;
    }

}