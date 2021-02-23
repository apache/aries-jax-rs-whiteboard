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
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Bundle.STOPPING;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleStateSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;

public abstract class BaseResource {

    final BundleContext bundleContext;
    final Bundle framework;

    @Context
    volatile UriInfo uriInfo;
    @Context
    volatile HttpHeaders headers;

    public BaseResource(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.framework = bundleContext.getBundle(0);
    }

    BundleStateSchema bundleStateSchema(Bundle bundle) {
        int state = bundle.getState();
        BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
        return BundleStateSchema.build(
            state, (bundleStartLevel.isActivationPolicyUsed() ? Bundle.START_ACTIVATION_POLICY : 0) |
                (((state & (STARTING|ACTIVE|STOPPING)) != 0) ?
                    (bundleStartLevel.isPersistentlyStarted() ? 0 : Bundle.START_TRANSIENT) : 0));
    }

    BundleSchema bundleSchema(Bundle bundle) {
        return BundleSchema.build(
            bundle.getBundleId(),
            bundle.getLastModified(),
            bundle.getState(),
            bundle.getSymbolicName(),
            bundle.getVersion().toString(),
            bundle.getLocation()
        );
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
            BaseResource::create
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
        return f -> sr ->
            f.matches(sr.properties);
    }

}
