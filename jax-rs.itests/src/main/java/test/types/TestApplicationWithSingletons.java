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

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Set;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class TestApplicationWithSingletons extends Application {

    @Override
    public Set<Object> getSingletons() {
        return Collections.<Object>singleton(new MyResource$$Proxy());
    }

    @Bind
    @Path("my")
    @Produces("text/plain")
    public static class MyResource {
        @GET
        public String get() {
            return "ok";
        }
    }

    @NameBinding
    @Retention(RUNTIME)
    @Target({TYPE, METHOD})
    public @interface Bind {
    }

    public static class MyResource$$Proxy extends MyResource {
    }
}
