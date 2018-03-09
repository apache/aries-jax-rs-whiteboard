package org.apache.aries.jax.rs.whiteboard.internal.cxf.sse;

import javax.ws.rs.sse.OutboundSseEvent.Builder;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

class SseImpl implements Sse {
    SseImpl() {
    }

    @Override
    public Builder newEventBuilder() {
        return new OutboundSseEventImpl.BuilderImpl();
    }

    @Override
    public SseBroadcaster newBroadcaster() {
        return new SseBroadcasterImpl();
    }
}