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

package org.apache.aries.jax.rs.whiteboard.internal.utils;

import org.apache.aries.osgi.functional.Effect;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LogUtils {

    public static Effect<Object> debugTracking(
        Logger log, Supplier<String> type) {

        return new Effect<>(
            o -> {
                if (log.isDebugEnabled()) {
                    log.debug("adding new {}: {}", type.get(), o);
                }
            },
            o -> {
                if (log.isDebugEnabled()) {
                    log.debug("removed new {}: {}", type.get(), o);
                }
            }
        );
    }

    public static Consumer<Object> ifDebugEnabled(
        Logger log, Supplier<String> message) {

        return o -> {
            if (log.isDebugEnabled()) {
                log.debug(message.get(), o);
            }
        };
    }

    public static Consumer<Object> ifInfoEnabled(
        Logger log, Supplier<String> message) {

        return o -> {
            if (log.isInfoEnabled()) {
                log.info(message.get(), o);
            }
        };
    }

    public static Consumer<Object> ifErrorEnabled(
        Logger log, Supplier<String> message) {

        return o -> {
            if (log.isErrorEnabled()) {
                log.error(message.get(), o);
            }
        };
    }

    public static Consumer<Object> ifErrorEnabled(
        Logger log, Supplier<String> message, Throwable t) {

        return o -> {
            if (log.isErrorEnabled()) {
                log.error(message.get(), o, t);
            }
        };
    }
}
