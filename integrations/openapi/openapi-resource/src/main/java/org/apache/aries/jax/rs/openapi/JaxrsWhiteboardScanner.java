package org.apache.aries.jax.rs.openapi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.jax.rs.whiteboard.ApplicationClasses;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;

import javax.ws.rs.core.Application;

public class JaxrsWhiteboardScanner implements OpenApiScanner {

    private final Application application;
    private final ApplicationClasses applicationClasses;
    public JaxrsWhiteboardScanner(Application application, ApplicationClasses applicationClasses) {
        this.application = application;
        this.applicationClasses = applicationClasses;
    }

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
    }

    @Override
    public Set<Class<?>> classes() {
        Set<Class<?>> classes = new HashSet<>(applicationClasses.classes());
        classes.add(application.getClass());
        return classes;
    }

    @Override
    public Map<String, Object> resources() {
        return null;
    }

}