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

package org.apache.aries.jax.rs.rest.management.internal;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.*;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.INSTALLED;
import static org.osgi.framework.Bundle.RESOLVED;
import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Bundle.STOPPING;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.aries.jax.rs.rest.management.model.BundleStateDTO;
import org.apache.aries.jax.rs.rest.management.model.BundlesDTO;
import org.apache.aries.jax.rs.rest.management.model.ServiceReferenceDTOs;
import org.apache.aries.jax.rs.rest.management.model.ServicesDTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

public class FrameworkResource {

    private final BundleContext bundleContext;
    private final Bundle framework;

    @Context
    volatile UriInfo uriInfo;
    @Context
    volatile HttpHeaders headers;

    public FrameworkResource(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.framework = bundleContext.getBundle(0);
    }

    @GET
    @Path("framework{type: (\\.json|\\.xml)*}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Response framework(@PathParam("type") String type) {
        ResponseBuilder builder = Response.status(
            Response.Status.OK
        ).entity(
            framework.adapt(FrameworkDTO.class)
        );

        return Optional.ofNullable(
            type
        ).map(
            String::trim
        ).map(
            t -> ".json".equals(t) ? APPLICATION_JSON : APPLICATION_XML
        ).map(t -> builder.type(t)).orElse(
            builder
        ).build();
    }

    @GET
    @Produces({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Path("framework/state{type:(\\.json|\\.xml)*}")
    public Response state(@PathParam("type") String type) {
        ResponseBuilder builder = Response.status(
            Response.Status.OK
        ).entity(
            state(framework)
        );

        return Optional.ofNullable(
            type
        ).map(
            String::trim
        ).map(
            t -> ".json".equals(t) ? APPLICATION_BUNDLESTATE_JSON : APPLICATION_BUNDLESTATE_XML
        ).map(t -> builder.type(t)).orElse(
            builder
        ).build();
    }

    // 137.3.1.1
    @GET
    @Produces({APPLICATION_FRAMEWORKSTARTLEVEL_JSON, APPLICATION_FRAMEWORKSTARTLEVEL_XML})
    @Path("framework/startlevel{type:(\\.json|\\.xml)*}")
    public Response startlevel(@PathParam("type") String type) {
        ResponseBuilder builder = Response.status(
            Response.Status.OK
        ).entity(
            framework.adapt(FrameworkStartLevelDTO.class)
        );

        return Optional.ofNullable(
            type
        ).map(
            String::trim
        ).map(
            t -> ".json".equals(t) ? APPLICATION_FRAMEWORKSTARTLEVEL_JSON : APPLICATION_FRAMEWORKSTARTLEVEL_XML
        ).map(t -> builder.type(t)).orElse(
            builder
        ).build();
    }

    // 137.3.1.2
    @PUT
    @Consumes({APPLICATION_FRAMEWORKSTARTLEVEL_JSON, APPLICATION_FRAMEWORKSTARTLEVEL_XML})
    @Path("framework/startlevel")
    public Response startlevel(FrameworkStartLevelDTO update) {
        try {
            FrameworkStartLevel current = framework.adapt(FrameworkStartLevel.class);

            if (current.getStartLevel() != update.startLevel) {
                current.setStartLevel(update.startLevel);
            }
            if (current.getInitialBundleStartLevel() != update.initialBundleStartLevel) {
                current.setInitialBundleStartLevel(update.initialBundleStartLevel);
            }

            return Response.noContent().build();
        }
        catch (IllegalArgumentException exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.2.1
    @GET
    @Produces({APPLICATION_BUNDLES_JSON, APPLICATION_BUNDLES_XML})
    @Path("framework/bundles")
    public BundlesDTO bundles(@Context UriInfo info) {
        Predicate<Bundle> predicate = fromNamespaceQuery(info);

        return BundlesDTO.build(
            Stream.of(
                bundleContext.getBundles()
            ).filter(
                predicate
            ).map(
                Bundle::getBundleId
            ).map(
                String::valueOf
            ).map(
                id -> uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(id).toASCIIString()
            ).collect(toList())
        );
    }

    // 137.3.2.2
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundles")
    public Response postBundles(String location) {
        try {
            Instant now = Instant.now().minusMillis(2);
            Bundle bundle = bundleContext.installBundle(location);

            if (!now.isBefore(Instant.ofEpochMilli(bundle.getLastModified()))) {
                throw new WebApplicationException(location, 409);
            }

            return Response.ok(
                bundle.adapt(BundleDTO.class)
            ).build();
        }
        catch (BundleException exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.2.3
    @POST
    @Consumes(APPLICATION_VNDOSGIBUNDLE)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundles")
    public Response postBundles(
        InputStream inputStream,
        @HeaderParam(HttpHeaders.CONTENT_LOCATION) String location) {

        try {
            location = Optional.ofNullable(location).orElseGet(
                () -> "org.apache.aries.jax.rs.whiteboard:".concat(
                    UUID.randomUUID().toString()));

            Instant now = Instant.now().minusMillis(2);
            Bundle bundle = bundleContext.installBundle(location, inputStream);

            if (!now.isBefore(Instant.ofEpochMilli(bundle.getLastModified()))) {
                throw new WebApplicationException(location, 409);
            }

            return Response.ok(
                bundle.adapt(BundleDTO.class)
            ).build();
        }
        catch (BundleException exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.3.1
    @GET
    @Produces({APPLICATION_BUNDLES_REPRESENTATIONS_JSON, APPLICATION_BUNDLES_REPRESENTATIONS_XML})
    @Path("framework/bundles/representations")
    public Collection<BundleDTO> bundleDTOs(@Context UriInfo info) {
        Predicate<Bundle> predicate = fromNamespaceQuery(info);

        return Stream.of(
            bundleContext.getBundles()
        ).filter(
            predicate
        ).map(
            b -> b.adapt(BundleDTO.class)
        ).collect(Collectors.toList());
    }

    // 137.3.4.1
    @GET
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}")
    public BundleDTO bundle(@PathParam("bundleid") long bundleid) {
        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        return bundle.adapt(BundleDTO.class);
    }

    // 137.3.4.2
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}")
    public Response postBundle(
        @PathParam("bundleid") long bundleid, String location) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException(String.valueOf(bundleid), 404);
            }

            if (location != null && !location.isEmpty()) {
                bundle.update(new URL(location).openStream());
            }
            else {
                bundle.update();
            }

            return Response.ok(
                bundle.adapt(BundleDTO.class)
            ).build();
        }
        catch (Exception exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.4.3
    @PUT
    @Consumes(APPLICATION_VNDOSGIBUNDLE)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}")
    public Response postBundle(
        @PathParam("bundleid") long bundleid, InputStream inputStream) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException(String.valueOf(bundleid), 404);
            }

            bundle.update(inputStream);

            return Response.ok(
                bundle.adapt(BundleDTO.class)
            ).build();
        }
        catch (Exception exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.4.4
    @DELETE
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}")
    public Response deleteBundle(@PathParam("bundleid") long bundleid) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException(String.valueOf(bundleid), 404);
            }

            bundle.uninstall();

            return Response.ok(
                bundle.adapt(BundleDTO.class)
            ).build();
        }
        catch (Exception exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.5.1
    @GET
    @Produces({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Path("framework/bundle/{bundleid}/state")
    public BundleStateDTO bundleState(@PathParam("bundleid") long bundleid) {
        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        return state(bundle);
    }

    // 137.3.5.2
    @PUT
    @Consumes({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Produces({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Path("framework/bundle/{bundleid}/state")
    public BundleStateDTO bundleState(
        @PathParam("bundleid") long bundleid,
        BundleStateDTO bundleStateDTO) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        int currentState = bundle.getState();

        try {
            if ((currentState & INSTALLED) == INSTALLED ||
                (currentState & RESOLVED) == RESOLVED) {
                if ((bundleStateDTO.state & ACTIVE) == ACTIVE) {
                    bundle.start(bundleStateDTO.options);
                }
                else if ((bundleStateDTO.state & RESOLVED) == RESOLVED) {
                    framework.adapt(
                        FrameworkWiring.class
                    ).resolveBundles(
                        Collections.singleton(bundle)
                    );
                }
            }
            else if ((currentState & ACTIVE) == ACTIVE) {
                if ((bundleStateDTO.state & RESOLVED) == RESOLVED) {
                    bundle.stop(bundleStateDTO.options);
                }
            }
            else {
                throw new WebApplicationException(
                    String.format(
                        "the requested target state [%s] is not reachable " +
                        "from the current bundle state [%s] or is not a " +
                        "target state",
                        bundleStateDTO.state, currentState), 402);
            }
        }
        catch (BundleException exception) {
            throw new WebApplicationException(exception, 400);
        }

        return state(bundle);
    }

    // 137.3.6.1
    @GET
    @Produces({APPLICATION_BUNDLEHEADER_JSON, APPLICATION_BUNDLEHEADER_XML})
    @Path("framework/bundle/{bundleid}/header")
    public Dictionary<String, String> bundleHeaders(
        @PathParam("bundleid") long bundleid) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        return bundle.getHeaders();
    }

    // 137.3.7.1
    @GET
    @Produces({APPLICATION_BUNDLESTARTLEVEL_JSON, APPLICATION_BUNDLESTARTLEVEL_XML})
    @Path("framework/bundle/{bundleid}/startlevel")
    public BundleStartLevelDTO bundleStartlevel(
        @PathParam("bundleid") long bundleid) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        return bundle.adapt(BundleStartLevelDTO.class);
    }

    // 137.3.7.2
    @PUT
    @Consumes({APPLICATION_BUNDLESTARTLEVEL_JSON, APPLICATION_BUNDLESTARTLEVEL_XML})
    @Produces({APPLICATION_BUNDLESTARTLEVEL_JSON, APPLICATION_BUNDLESTARTLEVEL_XML})
    @Path("framework/bundle/{bundleid}/startlevel")
    public Response bundleStartlevel(
        @PathParam("bundleid") long bundleid, BundleStartLevelDTO update) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        try {
            BundleStartLevel current = bundle.adapt(BundleStartLevel.class);

            if (current.getStartLevel() != update.startLevel) {
                current.setStartLevel(update.startLevel);
            }

            return Response.ok(bundle.adapt(BundleStartLevelDTO.class)).build();
        }
        catch (IllegalArgumentException exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

    // 137.3.8.1
    @GET
    @Produces({APPLICATION_SERVICES_JSON, APPLICATION_SERVICES_XML})
    @Path("framework/services")
    public ServicesDTO services(@QueryParam("filter") String filter) {
        Predicate<ServiceReferenceDTO> predicate = fromFilterQuery(filter);

        return ServicesDTO.build(
            framework.adapt(FrameworkDTO.class).services.stream().filter(
                predicate
            ).map(
                sr -> String.valueOf(sr.id)
            ).map(
                id -> uriInfo.getBaseUriBuilder().path("framework").path("service").path(id)
            ).map(
                UriBuilder::build
            ).map(
                URI::toASCIIString
            )
            .collect(toList())
        );
    }

    // 137.3.9.1
    @GET
    @Produces({APPLICATION_SERVICES_REPRESENTATIONS_JSON, APPLICATION_SERVICES_REPRESENTATIONS_XML})
    @Path("framework/services/representations")
    public ServiceReferenceDTOs serviceDTOs(@QueryParam("filter") String filter) {
        Predicate<ServiceReferenceDTO> predicate = fromFilterQuery(filter);

        return ServiceReferenceDTOs.build(
            framework.adapt(
                FrameworkDTO.class
            ).services.stream().filter(
                predicate
            ).collect(toList())
        );
    }

    // 137.3.9.1
    @GET
    @Produces({APPLICATION_SERVICE_JSON, APPLICATION_SERVICE_XML})
    @Path("framework/service/{serviceid}")
    public ServiceReferenceDTO service(
        @PathParam("serviceid") long serviceid) {

        return Stream.of(
            bundleContext.getBundles()
        ).flatMap(
            bundle -> Optional.ofNullable(
                bundle.adapt(ServiceReferenceDTO[].class)
            ).map(
                Stream::of
            ).orElseGet(
                Stream::empty
            )
        ).filter(
            sr -> sr.id == serviceid
        ).findFirst().orElseThrow(
            () -> new WebApplicationException(404)
        );
    }

    private BundleStateDTO state(Bundle bundle) {
        final BundleStateDTO bundleState = new BundleStateDTO();
        bundleState.state = bundle.getState();
        BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
        bundleState.options =
            (bundleStartLevel.isActivationPolicyUsed() ? Bundle.START_ACTIVATION_POLICY : 0) |
            (((bundleState.state & (STARTING|ACTIVE|STOPPING)) != 0) ?
                (bundleStartLevel.isPersistentlyStarted() ? 0 : Bundle.START_TRANSIENT) : 0);
        return bundleState;
    }

    static Predicate<Bundle> fromNamespaceQuery(UriInfo info) {
        MultivaluedMap<String,String> parameters = info.getQueryParameters(true);

        if (parameters.isEmpty()) {
            return b -> true;
        }

        Map<String, List<Filter>> filters = parameters.entrySet().stream().flatMap(
            e -> e.getValue().stream().map(v -> new SimpleEntry<>(e.getKey(), v))
        ).map(
            e -> new SimpleEntry<>(e.getKey(), create(e.getValue()))
        ).collect(
            groupingBy(Entry::getKey, mapping(Entry::getValue, toList()))
        );

        if (filters.isEmpty()) {
            return b -> true;
        }

        return b -> {
            BundleWiring wiring = b.adapt(BundleWiring.class);
            return filters.entrySet().stream().allMatch(
                entry -> {
                    String namespace = entry.getKey();
                    List<BundleCapability> caps = wiring.getCapabilities(namespace);
                    return entry.getValue().stream().anyMatch(
                        f ->
                            caps.stream().anyMatch(
                                cap ->
                                    f.matches(cap.getAttributes())
                            )
                    );
                }
            );
        };
    }

    static Predicate<ServiceReferenceDTO> fromFilterQuery(String filter) {
        return Optional.ofNullable(
            filter
        ).map(
            FrameworkResource::create
        ).map(
            toPredicate()
        ).orElseGet(
            () -> sr -> true
        );
    }

    static Filter create(String filter) {
        try {
            return FrameworkUtil.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            throw new WebApplicationException(
                String.format("Malformed filter [%s]", filter),
                Response.Status.BAD_REQUEST);
        }
    }

    static Function<Filter, Predicate<ServiceReferenceDTO>> toPredicate() {
        return f -> sr -> f.matches(new CaseInsensitiveMap(sr.properties));
    }


    /**
     * This Map is used for case-insensitive key lookup during filter
     * evaluation. This Map implementation only supports the get operation using
     * a String key as no other operations are used by the Filter
     * implementation.
     */
    private static final class CaseInsensitiveMap extends AbstractMap<String, Object> {
        private final String[] keys;
        private final Map<String, Object> properties;

        /**
         * Create a case insensitive map from the specified dictionary.
         *
         * @param properties
         * @throws IllegalArgumentException If {@code dictionary} contains case
         *             variants of the same key name.
         */
        CaseInsensitiveMap(Map<String, Object> properties) {
            this.properties = properties;
            List<String> keyList = new ArrayList<>(this.properties.size());
            this.properties.forEach((k,v) -> {
                for (String i : keyList) {
                    if (k.equalsIgnoreCase(i)) {
                        throw new IllegalArgumentException();
                    }
                }
                keyList.add(k);
            });
            this.keys = keyList.toArray(new String[0]);
        }

        @Override
        public Object get(Object o) {
            String k = (String) o;
            for (String key : keys) {
                if (key.equalsIgnoreCase(k)) {
                    return super.get(key);
                }
            }
            return null;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return this.properties.entrySet();
        }

    }

}
