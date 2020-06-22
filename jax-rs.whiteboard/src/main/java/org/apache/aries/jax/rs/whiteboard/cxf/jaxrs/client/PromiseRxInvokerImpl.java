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

package org.apache.aries.jax.rs.whiteboard.cxf.jaxrs.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.osgi.service.jaxrs.client.PromiseRxInvoker;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class PromiseRxInvokerImpl implements PromiseRxInvoker {

    private final Method _doInvokeAsyncMethod;

    private static final class DeferredHandler<R> implements InvocationCallback<R> {
        private final Deferred<R> deferred;

        private DeferredHandler(Deferred<R> deferred) {
            this.deferred = deferred;
        }

        @Override
        public void completed(R response) {
            deferred.resolve(response);
        }

        @Override
        public void failed(Throwable throwable) {
            deferred.fail(throwable);
        }
    }
    public PromiseRxInvokerImpl(
        WebClient webClient, PromiseFactory promiseFactory) {
        _webClient = webClient;
        try {
            _doInvokeAsyncMethod = WebClient.class.getDeclaredMethod(
                "doInvokeAsync",
                String.class, Object.class, Class.class, Type.class, Class.class, Type.class, InvocationCallback.class);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
        _doInvokeAsyncMethod.setAccessible(true);
        _promiseFactory = promiseFactory;
    }

    @Override
    public Promise<Response> delete() {
        return method(HttpMethod.DELETE);
    }

    @Override
    public <R> Promise<R> delete(Class<R> aClass) {
        return method(HttpMethod.DELETE, aClass);
    }

    @Override
    public <R> Promise<R> delete(GenericType<R> genericType) {
        return method(HttpMethod.DELETE, genericType);
    }

    @Override
    public Promise<Response> get() {
        return method(HttpMethod.GET);
    }

    @Override
    public <R> Promise<R> get(Class<R> aClass) {
        return method(HttpMethod.GET, aClass);
    }

    @Override
    public <R> Promise<R> get(GenericType<R> genericType) {
        return method(HttpMethod.GET, genericType);
    }

    @Override
    public Promise<Response> head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public <R> Promise<R> method(String s, Class<R> responseType) {
        Deferred<R> deferred = _promiseFactory.deferred();

        try {
            _doInvokeAsyncMethod.invoke(
                _webClient, s, null, null, null, responseType, responseType,
                new DeferredHandler<R>(deferred));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return deferred.getPromise();
    }

    @Override
    public <R> Promise<R> method(String s, Entity<?> entity, Class<R> responseType) {
        Deferred<R> deferred = _promiseFactory.deferred();

        try {
            _doInvokeAsyncMethod.invoke(
                _webClient, s, entity, null, null, responseType, responseType,
                new DeferredHandler<R>(deferred));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return deferred.getPromise();
    }

    @Override
    public <R> Promise<R> method(
        String s, Entity<?> entity, GenericType<R> genericType) {

        Deferred<R> deferred = _promiseFactory.deferred();

        try {
            _doInvokeAsyncMethod.invoke(
                _webClient, s, entity, null, null, genericType.getRawType(),
                genericType.getType(), new DeferredHandler<R>(deferred));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return deferred.getPromise();
    }

    @Override
    public Promise<Response> method(String s, Entity<?> entity) {
        return method(s, entity, Response.class);
    }

    @Override
    public <R> Promise<R> method(String s, GenericType<R> genericType) {
        Deferred<R> deferred = _promiseFactory.deferred();

        try {
            _doInvokeAsyncMethod.invoke(
                _webClient, s, null, null, null, genericType.getRawType(),
                genericType.getType(), new DeferredHandler<R>(deferred));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return deferred.getPromise();
    }

    @Override
    public Promise<Response> method(String s) {
        return method(s, Response.class);
    }

    @Override
    public Promise<Response> options() {
        return method(HttpMethod.OPTIONS);
    }

    @Override
    public <R> Promise<R> options(Class<R> aClass) {
        return method(HttpMethod.OPTIONS, aClass);
    }

    @Override
    public <R> Promise<R> options(GenericType<R> genericType) {
        return method(HttpMethod.OPTIONS, genericType);
    }

    @Override
    public <R> Promise<R> post(Entity<?> entity, Class<R> aClass) {
        return method(HttpMethod.POST, entity, aClass);
    }

    @Override
    public <R> Promise<R> post(Entity<?> entity, GenericType<R> genericType) {
        return method(HttpMethod.POST, entity, genericType);
    }

    @Override
    public Promise<Response> post(Entity<?> entity) {
        return method(HttpMethod.POST, entity);
    }

    @Override
    public <R> Promise<R> put(Entity<?> entity, Class<R> aClass) {
        return method(HttpMethod.PUT, entity, aClass);
    }

    @Override
    public <R> Promise<R> put(Entity<?> entity, GenericType<R> genericType) {
        return method(HttpMethod.PUT, entity, genericType);
    }

    @Override
    public Promise<Response> put(Entity<?> entity) {
        return method(HttpMethod.PUT, entity);
    }

    @Override
    public Promise<Response> trace() {
        return method("TRACE", Response.class);
    }

    @Override
    public <R> Promise<R> trace(Class<R> aClass) {
        return method("TRACE", aClass);
    }

    @Override
    public <R> Promise<R> trace(GenericType<R> genericType) {
        return method("TRACE", genericType);
    }

    private final PromiseFactory _promiseFactory;
    private final WebClient _webClient;
}
