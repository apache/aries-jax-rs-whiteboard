package org.apache.aries.jax.rs.whiteboard.internal.cxf.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

public class SseBroadcasterImpl implements SseBroadcaster {
    private final Set<SseEventSink> subscribers = new CopyOnWriteArraySet<>();

    private final Set<Consumer<SseEventSink>> closers =
            new CopyOnWriteArraySet<>();

    private final Set<BiConsumer<SseEventSink, Throwable>> exceptioners =
            new CopyOnWriteArraySet<>();

    @Override
    public void register(SseEventSink sink) {
        if (closed) throw new IllegalStateException("Already closed");

        SseEventSinkImpl sinkImpl = (SseEventSinkImpl)sink;

        AsyncContext asyncContext = sinkImpl.getAsyncContext();

        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent asyncEvent) throws IOException {
                subscribers.remove(sink);
            }

            @Override
            public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                subscribers.remove(sink);
            }

            @Override
            public void onError(AsyncEvent asyncEvent) throws IOException {
                subscribers.remove(sink);
            }

            @Override
            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {

            }
        });

        subscribers.add(sink);
    }

    @Override
    public CompletionStage<?> broadcast(OutboundSseEvent event) {
        if (closed) throw new IllegalStateException("Already closed");

        final Collection<CompletableFuture<?>> futures = new ArrayList<>();
        
        for (SseEventSink sink: subscribers) {
            try {
                futures.add(sink.send(event).toCompletableFuture());
            } catch (final Exception ex) {
                exceptioners.forEach(
                    exceptioner -> exceptioner.accept(sink, ex));
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    @Override
    public void onClose(Consumer<SseEventSink> subscriber) {
        if (closed) throw new IllegalStateException("Already closed");

        closers.add(subscriber);
    }

    @Override
    public void onError(BiConsumer<SseEventSink, Throwable> exceptioner) {
        if (closed) throw new IllegalStateException("Already closed");

        exceptioners.add(exceptioner);
    }

    @Override
    public void close() {
        closed = true;

        subscribers.forEach(subscriber -> {
            subscriber.close();
            closers.forEach(closer -> closer.accept(subscriber));
        });
    }

    private volatile boolean closed;
}