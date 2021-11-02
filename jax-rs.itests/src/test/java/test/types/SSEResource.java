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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class SSEResource {

    @Produces(MediaType.SERVER_SENT_EVENTS)
    @GET
    @Path("/subscribe")
    public void subscribe(@Context SseEventSink sink) {
        sink.send(_sse.newEvent("welcome"));

        _sseBroadcaster.register(sink);
    }

    @POST
    @Path("/broadcast")
    public void broadcast(String body)
        throws ExecutionException, InterruptedException {

        CompletionStage<?> broadcast = _sseBroadcaster.broadcast(
            _sse.newEvent(body));

        broadcast.toCompletableFuture().get();
    }

    @Context
    public void setSse(Sse sse) {
        _sse = sse;
        _sseBroadcaster = _sse.newBroadcaster();
    }

    private Sse _sse;
    private SseBroadcaster _sseBroadcaster;
    
}
