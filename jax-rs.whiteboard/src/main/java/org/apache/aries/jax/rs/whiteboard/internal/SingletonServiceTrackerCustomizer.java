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

package org.apache.aries.jax.rs.whiteboard.internal;

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * @author Carlos Sierra Andrés
 */
class SingletonServiceTrackerCustomizer
	implements ServiceTrackerCustomizer
		<Object, SingletonServiceTrackerCustomizer.Tracked> {

	private BundleContext _bundleContext;
	private Bus _bus;

	public SingletonServiceTrackerCustomizer(
		BundleContext bundleContext, Bus bus) {

		_bundleContext = bundleContext;
		_bus = bus;
	}

	@Override
	public Tracked addingService(
		ServiceReference<Object> serviceReference) {

		final Object service = _bundleContext.getService(
			serviceReference);

		try {
			String[] propertyKeys = serviceReference.getPropertyKeys();

			Map<String, Object> properties = new HashMap<>(
				propertyKeys.length);

			for (String propertyKey : propertyKeys) {
				if (propertyKey.equals("osgi.jaxrs.resource.base")) {
					continue;
				}
				properties.put(
					propertyKey, serviceReference.getProperty(propertyKey));
			}

			properties.put(
				"CXF_ENDPOINT_ADDRESS",
				serviceReference.getProperty("osgi.jaxrs.resource.base").
					toString());

			CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
				new CXFJaxRsServiceRegistrator(
					_bus,
					new Application() {
						@Override
						public Set<Object> getSingletons() {
							return Collections.singleton(service);
						}
					},
					properties);

			return new Tracked(
				cxfJaxRsServiceRegistrator, service,
				_bundleContext.registerService(
					CXFJaxRsServiceRegistrator.class,
					cxfJaxRsServiceRegistrator, new Hashtable<>(properties)));
		}
		catch (Exception e) {
			_bundleContext.ungetService(serviceReference);

			throw e;
		}
	}

	@Override
	public void modifiedService(
		ServiceReference<Object> serviceReference, Tracked tracked) {

		removedService(serviceReference, tracked);

		addingService(serviceReference);
	}

	@Override
	public void removedService(
		ServiceReference<Object> reference, Tracked tracked) {

		_bundleContext.ungetService(reference);

		Object service = tracked.getService();

		CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
			tracked.getCxfJaxRsServiceRegistrator();

		cxfJaxRsServiceRegistrator.close();

		tracked.getCxfJaxRsServiceRegistratorServiceRegistration().unregister();
	}

	public static class Tracked {

		private final CXFJaxRsServiceRegistrator _cxfJaxRsServiceRegistrator;
		private final Object _service;
		private final ServiceRegistration<CXFJaxRsServiceRegistrator>
			_cxfJaxRsServiceRegistratorServiceRegistration;

		public Object getService() {
			return _service;
		}

		public CXFJaxRsServiceRegistrator getCxfJaxRsServiceRegistrator() {
			return _cxfJaxRsServiceRegistrator;
		}

		public ServiceRegistration<CXFJaxRsServiceRegistrator>
			getCxfJaxRsServiceRegistratorServiceRegistration() {

			return _cxfJaxRsServiceRegistratorServiceRegistration;
		}

		public Tracked(
			CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator,
			Object service,
			ServiceRegistration<CXFJaxRsServiceRegistrator>
				cxfJaxRsServiceRegistratorServiceRegistration) {

			_cxfJaxRsServiceRegistrator = cxfJaxRsServiceRegistrator;
			_service = service;
			_cxfJaxRsServiceRegistratorServiceRegistration =
				cxfJaxRsServiceRegistratorServiceRegistration;
		}

	}

}


