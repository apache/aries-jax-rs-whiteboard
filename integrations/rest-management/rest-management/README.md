## Apache Aries JAX-RS Rest Management Service

Welcome to the Apache Aries JAX-RS OSGi REST Management integration. This integration implements **Chapter 137** of the **OSGi R7 Compendium** [REST Management Service Specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.rest.html).

In essence this integration allows for management and introspection of the OSGi framework over REST.

### Rest Management Service Location

Since there should only be one management API per framework the integration creates a separate application rooted at `${osgi.jaxrs.endpoint}/rms`

Some of the available paths are:

```
/rms/framework/bundle/{bundleid}/header
/rms/framework/bundle/{bundleid}
/rms/framework/bundle/{bundleid}/startlevel
/rms/framework/bundle/{bundleid}/state
/rms/framework/bundles/representations
/rms/framework/bundles
/rms/framework
/rms/framework/service/{serviceid}
/rms/framework/services/representations
/rms/framework/services
/rms/framework/startlevel
/rms/framework/state
/rms/extensions
```

### Open API

To simplify developer experience there is an Open API endpoint mounted at `${osgi.jaxrs.endpoint}/rms/openapi.(json|yaml)`.

#### TODO

- fix conditional response type based on extension logic to account for mediatype resource method parameters

##### Wish list

- add HATEOS-style links within representations that allow for references to associated resources (e.g. in bundle representation add links to state, startlevel, headers)
- add a extensions/inspect[/{bundleid}] (requirements and capabilities) resource (akin to gogo inspect command)
- add a extension/repository that returns the [XML Repository Format](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html#i3247820)
- add a extension/cm* for integration for Configuration admin (uses [Configuration Resource Format](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html#d0e131566))