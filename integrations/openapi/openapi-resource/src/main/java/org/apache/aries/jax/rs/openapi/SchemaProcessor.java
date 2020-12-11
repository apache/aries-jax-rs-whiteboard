package org.apache.aries.jax.rs.openapi;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

public interface SchemaProcessor {

    public void process(Schema<?> schema, AnnotatedType type, ModelConverterContext context);

}
