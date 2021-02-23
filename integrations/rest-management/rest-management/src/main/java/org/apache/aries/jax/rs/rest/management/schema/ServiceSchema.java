package org.apache.aries.jax.rs.rest.management.schema;

import java.util.Map;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.osgi.framework.dto.ServiceReferenceDTO;

@XmlRootElement(name = "service")
@XmlJavaTypeAdapter(ServiceSchemaAdapter.class)
public class ServiceSchema extends ServiceReferenceDTO {

    public String bundle;
    public String[] usingBundles;

    public static ServiceSchema build(
        String bundle, long id, Map<String, Object> properties, String[] usingBundles) {
        final ServiceSchema serviceSchema = new ServiceSchema();
        serviceSchema.bundle = bundle;
        serviceSchema.id = id;
        serviceSchema.properties = properties;
        serviceSchema.usingBundles = usingBundles;
        return serviceSchema;
    }

    public static ServiceSchema build(UriInfo uriInfo, ServiceReferenceDTO serviceReferenceDTO) {
        final ServiceSchema serviceSchema = new ServiceSchema();
        serviceSchema.bundle = uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(serviceReferenceDTO.bundle).toASCIIString();
        serviceSchema.id = serviceReferenceDTO.id;
        serviceSchema.properties = serviceReferenceDTO.properties;
        serviceSchema.usingBundles = new String[serviceReferenceDTO.usingBundles.length];
        for (int i = 0; i < serviceReferenceDTO.usingBundles.length; i++) {
            serviceSchema.usingBundles[i] = uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(serviceReferenceDTO.usingBundles[i]).toASCIIString();
        }
        return serviceSchema;
    }

}
