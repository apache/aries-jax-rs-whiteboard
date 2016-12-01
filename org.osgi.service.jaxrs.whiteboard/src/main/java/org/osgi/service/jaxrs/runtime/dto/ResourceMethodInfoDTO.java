package org.osgi.service.jaxrs.runtime.dto;

import org.osgi.dto.DTO;

/**
 * Represents information about a JAX-RS resource method.
 *
 * @NotThreadSafe
 * @author $Id: 55683e4bb50d121a46c8c94d315d9ab0a051195f $
 */
public class ResourceMethodInfoDTO extends DTO {

	/**
	 * The HTTP verb being handled, for example GET, DELETE, PUT, POST, HEAD,
	 * OPTIONS
	 */
	String	method;

	/**
	 * The mime-type(s) consumed by this resource method, null if not defined
	 */
	String[]	consumingMimeType;

	/**
	 * The mime-type(s) produced by this resource method, null if not defined
	 */
	String[]	producingMimeType;

	/**
	 * The URI of this sub-resource, null if this is not a sub-resource method
	 */
	String	uri;
}
