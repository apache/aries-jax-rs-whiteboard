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
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.osgi.service.jaxrs.client.PromiseRxInvoker;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

class PromiseRxInvokerImpl implements PromiseRxInvoker {

    public PromiseRxInvokerImpl(
        SyncInvoker syncInvoker, PromiseFactory promiseFactory) {
        _syncInvoker = syncInvoker;
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
    	return _promiseFactory.submit(() -> _syncInvoker.method(s, responseType));
    }

    @Override
    public <R> Promise<R> method(String s, Entity<?> entity, Class<R> responseType) {
    	return _promiseFactory.submit(() -> _syncInvoker.method(s, entity, responseType));
    }

    @Override
    public <R> Promise<R> method(
        String s, Entity<?> entity, GenericType<R> genericType) {
    	return _promiseFactory.submit(() -> _syncInvoker.method(s, entity, genericType));
    }

    @Override
    public Promise<Response> method(String s, Entity<?> entity) {
        return method(s, entity, Response.class);
    }

    @Override
    public <R> Promise<R> method(String s, GenericType<R> genericType) {
    	return _promiseFactory.submit(() -> _syncInvoker.method(s, genericType));
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
    private final SyncInvoker _syncInvoker;
}
