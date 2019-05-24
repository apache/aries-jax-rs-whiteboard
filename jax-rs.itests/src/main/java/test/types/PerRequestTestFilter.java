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

package test.types;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.io.IOException;

public class PerRequestTestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext)
        throws IOException {

        Class<?> matchedResource = (Class<?>)requestContext.
            getUriInfo().
            getMatchedResources().
            get(0);

        Object resource = _resourceContext.getResource(matchedResource);

        if (resource instanceof PerRequestTestResource) {
            PerRequestTestResource perRequestTestResource =
                (PerRequestTestResource) resource;

            perRequestTestResource.setState(perRequestTestResource.getState() + "-changed");
        }
    }

    @Context
    private ResourceContext _resourceContext;
}
