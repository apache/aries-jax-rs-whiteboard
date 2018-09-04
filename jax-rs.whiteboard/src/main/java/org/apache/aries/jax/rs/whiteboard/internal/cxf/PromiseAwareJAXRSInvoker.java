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
package org.apache.aries.jax.rs.whiteboard.internal.cxf;

import java.util.Arrays;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.message.Message;
import org.osgi.util.promise.Promise;

public class PromiseAwareJAXRSInvoker extends JAXRSInvoker {
    
    /**
     * OSGi promises are a great way to do asynchronous work, and should be handled
     * natively just like a CompletionStage
     */
    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        
        // Fast path - do they share our view of the Promise
        if (result instanceof Promise) {
            return handlePromise(inMessage, (Promise<?>) result);
        } 
        
        // Slower check, is it a Promise?
        Class<?> clazz = result.getClass();
        if(Arrays.stream(clazz.getInterfaces())
            .map(Class::getName)
            .anyMatch(n -> "org.osgi.util.promise.Promise".equals(n))) {
            
            return handlePromiseFromAnotherClassSpace(inMessage, result, clazz);
        }
        
        return super.checkFutureResponse(inMessage, result);
    }

    private AsyncResponseImpl handlePromise(Message inMessage, final Promise<?> promise) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        promise.onSuccess(asyncResponse::resume)
               .onFailure(asyncResponse::resume);
        return asyncResponse;
    }

    private AsyncResponseImpl handlePromiseFromAnotherClassSpace(Message inMessage, Object result, Class<?> clazz) {
        // It's a promise, but from a different class space. Use reflection to 
        // register a callback with the promise
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        try {
            clazz.getMethod("onResolve", Runnable.class).invoke(result, (Runnable) () -> {
                    try {
                        Object failure = clazz.getMethod("getFailure").invoke(result);
                        
                        if(failure != null) {
                            asyncResponse.resume((Throwable) failure);
                        } else {
                            asyncResponse.resume(clazz.getMethod("getValue").invoke(result));
                        }
                    } catch (Exception e) {
                        asyncResponse.resume(e);
                    }
                });
        } catch (Exception e) {
            asyncResponse.resume(e);
        }
        return asyncResponse;
    }
}
