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

package org.apache.aries.jax.rs.rest.management.handler;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings({ "serial", "rawtypes" })
class DictionaryJsonSerializer extends StdSerializer<Dictionary> {

    protected DictionaryJsonSerializer() {
        this(null);
    }

    protected DictionaryJsonSerializer(Class<Dictionary> t) {
        super(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(
            Dictionary value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {

        gen.writeStartObject();

        Collections.<String>list(value.keys()).stream().sorted().forEach(
            key -> {
                try {
                    gen.writeStringField(
                        String.valueOf(key),
                        String.valueOf(value.get(key))
                    );
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );

        gen.writeEndObject();
    }

}