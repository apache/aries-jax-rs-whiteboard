package org.apache.aries.jax.rs.whiteboard.internal.cxf.sse;

import javax.ws.rs.sse.Sse;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;

public class SseContextProvider implements ContextProvider<Sse> {
    @Override
    public Sse createContext(Message message) {
        return new SseImpl();
    }
}