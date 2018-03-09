package org.apache.aries.jax.rs.whiteboard.internal.cxf.sse;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.common.logging.LogUtils;

public class SseEventSinkImpl implements SseEventSink {
    private static final Logger LOG = LogUtils.getL7dLogger(SseEventSinkImpl.class);

    public AsyncContext getAsyncContext() {
        return ctx;
    }

    private final AsyncContext ctx;

    private static class QueuedEvent {
    		final OutboundSseEvent event;
    		final CompletableFuture<?> completion;
    		
		public QueuedEvent(OutboundSseEvent event, CompletableFuture<?> completion) {
			this.event = event;
			this.completion = completion;
		}
    }
    
    private final MessageBodyWriter<OutboundSseEvent> writer;
    private final Queue<QueuedEvent> queuedEvents;
    private boolean dequeueing;
    
    private volatile boolean closed;

    public SseEventSinkImpl(final MessageBodyWriter<OutboundSseEvent> writer,
            final AsyncContext ctx) {
        this.writer = writer;
        this.queuedEvents = new LinkedList<>();
        this.ctx = ctx;

        if (ctx == null) {
            throw new IllegalStateException("Unable to retrieve the AsyncContext for this request. "
                    + "Is the Servlet configured properly?");
        }
        
        ctx.getResponse().setContentType(OutboundSseEventBodyWriter.SERVER_SENT_EVENTS);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;

            try {
                ctx.complete();
            } catch (final Exception ex) {
                LOG.warning("Failed to close the AsyncContext cleanly: "
                    + ex.getMessage());
            }
        }
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        final CompletableFuture<?> future = new CompletableFuture<>();
        
        if (!closed && writer != null) {
        	    
            boolean startDequeue;
            synchronized (this) {
                queuedEvents.offer(new QueuedEvent(event, future));
                if(dequeueing) {
                    startDequeue = false;
                } else {
                    startDequeue = true;
                    dequeueing = true;
                }
            }
        		
            if(startDequeue) {
                ctx.start(this::dequeue);
            }
        }
        else {
            future.complete(null);
        }

        return future;
    }
    
    private void dequeue() {
    	
    		for(;;) {
    			QueuedEvent qe;
    			synchronized (this) {
	    			qe = queuedEvents.poll();
    				if(qe == null) {
    					dequeueing = false;
    					break;
    				}
    			}
			OutboundSseEvent event = qe.event;
			CompletableFuture<?> future = qe.completion;
			
			try {
                writer.writeTo(event, event.getClass(), event.getGenericType(), new Annotation [] {}, event.getMediaType(), null, ctx.getResponse().getOutputStream());
                ctx.getResponse().flushBuffer();
                future.complete(null);

			} catch (final Exception ex) {
				future.completeExceptionally(ex);
			}
    			
    		}
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}