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
import java.io.InputStream;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class XMLDeserializer {

    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    public <T> void deserialize(
            BiFunction<StartElement, XMLEventReader, XMLEvent> startConsumer,
            Consumer<EndElement> endConsumer, InputStream entityStream)
        throws IOException {

        try {
            XMLEventReader eventReader = inputFactory.createXMLEventReader(entityStream);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    event = startConsumer.apply(event.asStartElement(), eventReader);
                }

                if (event.isEndElement()) {
                    endConsumer.accept(event.asEndElement());
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

}