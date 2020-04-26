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

package org.apache.aries.jax.rs.whiteboard.internal.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.client.Client;

import org.apache.cxf.jaxrs.client.PromiseRxInvokerProviderImpl;

public class ClientBuilderImpl extends org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl {

    private final Set<Client> clients = ConcurrentHashMap.newKeySet();

    public ClientBuilderImpl() {
        super();

        register(new PromiseRxInvokerProviderImpl());
    }

    @Override
    public Client build() {
        final Client client = super.build();
        clients.add(client);
        return client;
    }

    void close() {
        clients.removeIf(c -> {c.close(); return true;});
    }

}
