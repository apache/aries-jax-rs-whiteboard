package org.apache.aries.jax.rs.whiteboard.internal;

import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET;

import java.util.function.Predicate;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetFilter<T> implements Predicate<ServiceReference<T>>  {

    public TargetFilter(ServiceReference<?> serviceRuntimeReference) {
        _serviceRuntimeReference = serviceRuntimeReference;
    }

    @Override
    public boolean test(ServiceReference<T> ref) {
        String target = (String)ref.getProperty(JAX_RS_WHITEBOARD_TARGET);

        if (target == null) {
            return true;
        }

        Filter filter;

        try {
            filter = FrameworkUtil.createFilter(target);
        }
        catch (InvalidSyntaxException ise) {
            if (_log.isErrorEnabled()) {
                _log.error("Invalid '{}' syntax in {}", JAX_RS_WHITEBOARD_TARGET, ref, ise);
            }

            return false;
        }

        return filter.match(_serviceRuntimeReference);
    }

    private static final Logger _log = LoggerFactory.getLogger(TargetFilter.class);

    private final ServiceReference<?> _serviceRuntimeReference;

}