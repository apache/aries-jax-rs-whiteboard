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

package test;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT;

import java.util.Hashtable;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;

import test.types.TestHelper;
import test.types.TestShiroAnnotations;

@Capability(namespace="osgi.service", attribute="objectClass=org.apache.shiro.realm.Realm")
public class ShiroTest extends TestHelper {

    private Configuration authzConfig, authcConfig;

    SimpleAccountRealm realm;

    ServiceRegistration<Realm> reg;

    @Before
    public void setupConfigs() throws Exception {
        authzConfig = getConfigurationAdmin()
                .getConfiguration("org.apache.aries.jax.rs.shiro.authorization");

        Hashtable<String, Object> table = new Hashtable<>();
        table.put("shiro.authz", TRUE);
        table.put(JAX_RS_EXTENSION_SELECT, "(shiro.authc=true)");
        authzConfig.update(table);

        authcConfig = getConfigurationAdmin()
                .getConfiguration("org.apache.aries.jax.rs.shiro.authentication");

        table = new Hashtable<>();
        table.put("shiro.authc", TRUE);
        authcConfig.update(table);

        realm = new SimpleAccountRealm();

        reg = bundleContext.registerService(Realm.class, realm, null);

        Thread.sleep(1000);
    }

    @After
    public void cleanUp() throws Exception {
        authzConfig.delete();

        authcConfig.delete();

        reg.unregister();
    }

    @Test
    public void testGuestNoAuthPresent() throws Exception {
        WebTarget webTarget = createDefaultTarget().path("test/guest");

        registerAddon(new TestShiroAnnotations(), JAX_RS_EXTENSION_SELECT, "(shiro.authz=true)");

        assertEquals("Welcome Guest", webTarget.request().get(String.class));
    }

    @Test
    public void testAuthenticatedNoAuthPresent() throws Exception {
        WebTarget webTarget = createDefaultTarget().path("test/authenticated");

        registerAddon(new TestShiroAnnotations(), JAX_RS_EXTENSION_SELECT, "(shiro.authz=true)");

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), webTarget.request().get().getStatus());
    }


    @Test
    public void testGuestAuthPresent() throws Exception {

        realm.addAccount("Bill", "Ben");

        WebTarget target = createDefaultTarget();

        registerAddon(new TestShiroAnnotations(), JAX_RS_EXTENSION_SELECT, "(shiro.authz=true)");

        Response authenticate = target.path("security/authenticate").request()
                .header("user", "Bill")
                .header("password", "Ben")
                .post(null);
        assertEquals(Status.OK.getStatusCode(), authenticate.getStatus());


        Builder request = target.path("test/guest").request();
        authenticate.getCookies().values().forEach(c -> request.cookie(c.getName(), c.getValue()));

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), request.get().getStatus());
    }

    @Test
    public void testAuthenticatedAuthPresent() throws Exception {

        realm.addAccount("Bill", "Ben");

        WebTarget target = createDefaultTarget();

        registerAddon(new TestShiroAnnotations(), JAX_RS_EXTENSION_SELECT, "(shiro.authz=true)");

        Response authenticate = target.path("security/authenticate").request()
                    .header("user", "Bill")
                    .header("password", "Ben")
                    .post(null);
        assertEquals(Status.OK.getStatusCode(), authenticate.getStatus());


        Builder request = target.path("test/authenticated").request();
        authenticate.getCookies().values().forEach(c -> request.cookie(c.getName(), c.getValue()));

        assertEquals("Welcome Bill", request.get(String.class));
    }

    @Test
    public void testRoleAuthPresent() throws Exception {

        realm.addAccount("Bill", "Ben");

        WebTarget target = createDefaultTarget();

        registerAddon(new TestShiroAnnotations(), JAX_RS_EXTENSION_SELECT, "(shiro.authz=true)");

        Response authenticate = target.path("security/authenticate").request()
                .header("user", "Bill")
                .header("password", "Ben")
                .post(null);
        assertEquals(Status.OK.getStatusCode(), authenticate.getStatus());


        Builder request = target.path("test/admin").request();
        authenticate.getCookies().values().forEach(c -> request.cookie(c.getName(), c.getValue()));

        assertEquals(Status.FORBIDDEN.getStatusCode(), request.get().getStatus());
    }

    @Test
    public void testRoleAuthWithRolePresent() throws Exception {

        realm.addAccount("Bill", "Ben", "admin");

        WebTarget target = createDefaultTarget();

        registerAddon(new TestShiroAnnotations(), JAX_RS_EXTENSION_SELECT, "(shiro.authz=true)");

        Response authenticate = target.path("security/authenticate").request()
                .header("user", "Bill")
                .header("password", "Ben")
                .post(null);
        assertEquals(Status.OK.getStatusCode(), authenticate.getStatus());


        Builder request = target.path("test/admin").request();
        authenticate.getCookies().values().forEach(c -> request.cookie(c.getName(), c.getValue()));

        assertEquals("Welcome Admin", request.get(String.class));
    }

}
