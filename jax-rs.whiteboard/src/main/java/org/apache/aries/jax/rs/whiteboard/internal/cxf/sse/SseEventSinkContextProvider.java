package org.apache.aries.jax.rs.whiteboard.internal.cxf.sse;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class SseEventSinkContextProvider implements ContextProvider<SseEventSink> {

    @Override
    public SseEventSink createContext(Message message) {
        final HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null) {
            throw new IllegalStateException("Unable to retrieve HTTP request from the context");
        }

        final MessageBodyWriter<OutboundSseEvent> writer = new OutboundSseEventBodyWriter(
            ServerProviderFactory.getInstance(message), message.getExchange());

        AsyncContext ctx = request.startAsync();
        ctx.setTimeout(0);

        message.getInterceptorChain().add(new SuspendPhaseInterceptor());

        return new SseEventSinkImpl(writer, ctx);
    }

    private static class SuspendPhaseInterceptor
        implements PhaseInterceptor<Message> {

        @Override
        public Set<String> getAfter() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getBefore() {
            return Collections.singleton(
                "org.apache.cxf.interceptor.OutgoingChainInterceptor");
        }

        @Override
        public String getId() {
            return "SSE SUSPEND";
        }

        @Override
        public String getPhase() {
            return Phase.POST_INVOKE;
        }

        @Override
        public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
            return Collections.emptySet();
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            message.getInterceptorChain().suspend();
        }

        @Override
        public void handleFault(Message message) {
        }

    }
}