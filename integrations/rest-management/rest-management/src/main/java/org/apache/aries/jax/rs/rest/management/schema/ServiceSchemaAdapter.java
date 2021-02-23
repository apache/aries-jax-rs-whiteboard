package org.apache.aries.jax.rs.rest.management.schema;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class ServiceSchemaAdapter extends XmlAdapter<ServiceSchemaAdapter.ServiceSchemaAdapted, ServiceSchema> {

    @XmlRootElement(name = "service")
    public static class ServiceSchemaAdapted {
        public long id;
        public String bundle;
        @XmlJavaTypeAdapter(PropertiesAdapter.class)
        public Map<String, Object> properties;
        public String[] usingBundles;
    }

    @Override
    public ServiceSchemaAdapted marshal(ServiceSchema serviceSchema) throws Exception {
        ServiceSchemaAdapted adapter = new ServiceSchemaAdapted();
        adapter.id = serviceSchema.id;
        adapter.bundle = serviceSchema.bundle;
        adapter.properties = serviceSchema.properties;
        adapter.usingBundles = serviceSchema.usingBundles;
        return adapter;
    }

    @Override
    public ServiceSchema unmarshal(ServiceSchemaAdapted adapter) throws Exception {
        ServiceSchema serviceSchema = new ServiceSchema();
        serviceSchema.id = adapter.id;
        serviceSchema.bundle = adapter.bundle;
        serviceSchema.properties = adapter.properties;
        serviceSchema.usingBundles = adapter.usingBundles;
        return serviceSchema;
    }

}
