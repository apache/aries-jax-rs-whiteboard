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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class FiltersAndInterceptorsServiceTrackerCustomizer
	implements ServiceTrackerCustomizer<Object, ServiceTracker<?, ?>> {

	private BundleContext _bundleContext;

	public FiltersAndInterceptorsServiceTrackerCustomizer(
		BundleContext bundleContext) {

		_bundleContext = bundleContext;
	}

	@Override
	public ServiceTracker<?, ?> addingService(final ServiceReference<Object> reference) {
		final String filterBase =
			reference.getProperty("osgi.jaxrs.filter.base").toString();

		final Object service = _bundleContext.getService(reference);

		ServiceTracker<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator> serviceTracker = new ServiceTracker<>(
			_bundleContext, CXFJaxRsServiceRegistrator.class,
			new ServiceTrackerCustomizer
				<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>() {

				@Override
				public CXFJaxRsServiceRegistrator addingService(
					ServiceReference<CXFJaxRsServiceRegistrator> cxfReference) {

					Object resourceBaseObject =
						cxfReference.getProperty("CXF_ENDPOINT_ADDRESS");

					if (resourceBaseObject == null) {
						return null;
					}

					String resourceBase = resourceBaseObject.toString();

					if (resourceBase.startsWith(filterBase)) {
						CXFJaxRsServiceRegistrator serviceRegistrator =
							_bundleContext.getService(cxfReference);
						try {
							serviceRegistrator.addProvider(service);

							return serviceRegistrator;
						}
						finally {
							_bundleContext.ungetService(reference);
						}
					}

					return null;
				}

				@Override
				public void modifiedService(
					ServiceReference<CXFJaxRsServiceRegistrator> reference,
					CXFJaxRsServiceRegistrator service) {

					removedService(reference, service);
					addingService(reference);
				}

				@Override
				public void removedService(
					ServiceReference<CXFJaxRsServiceRegistrator> reference,
					CXFJaxRsServiceRegistrator service) {

					CXFJaxRsServiceRegistrator serviceRegistrator =
						_bundleContext.getService(reference);
					try {
						serviceRegistrator.removeProvider(service);
					}
					finally {
						_bundleContext.ungetService(reference);
					}
				}
			});

		serviceTracker.open();

		return serviceTracker;
	}

	@Override
	public void modifiedService(
		ServiceReference<Object> reference, ServiceTracker<?, ?> serviceTracker) {

		removedService(reference, serviceTracker);
		addingService(reference);
	}

	@Override
	public void removedService(
		ServiceReference<Object> reference, ServiceTracker<?, ?> serviceTracker) {

		_bundleContext.ungetService(reference);

		serviceTracker.close();
	}
}
