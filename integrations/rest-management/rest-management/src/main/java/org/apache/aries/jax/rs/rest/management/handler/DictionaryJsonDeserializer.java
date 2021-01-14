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
import java.util.Dictionary;
import java.util.Hashtable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@SuppressWarnings("serial")
class DictionaryJsonDeserializer extends StdDeserializer<Dictionary<String, String>> {

    protected DictionaryJsonDeserializer() {
        this(null);
    }

    protected DictionaryJsonDeserializer(Class<Dictionary<String, String>> t) {
        super(t);
    }

    @Override
    public Dictionary<String, String> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {

        Dictionary<String, String> dictionary = new Hashtable<>();
        JsonNode node = jp.getCodec().readTree(jp);
        node.fields().forEachRemaining(entry -> dictionary.put(entry.getKey(), entry.getValue().asText()));
        return dictionary;
    }

}