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

package org.apache.aries.jax.rs.jaxb.json.activator;

import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.codehaus.jettison.mapped.TypeConverter;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Optional;

@Capability(
    attribute = {
        "objectClass:List<String>='javax.ws.rs.ext.MessageBodyReader,javax.ws.rs.ext.MessageBodyWriter'",
        "osgi.jaxrs.media.type=application/json",
        "osgi.jaxrs.name=cxf.jettison"
    },
    namespace = ServiceNamespace.SERVICE_NAMESPACE
)
@RequireJaxrsWhiteboard
public class JsonProviderPrototypeServiceFactory
    implements PrototypeServiceFactory<JSONProvider<?>> {

    JsonProviderPrototypeServiceFactory(
        Dictionary<String, ?> properties,
        Optional<TypeConverter> typeConverter,
        Optional<Marshaller.Listener> marshallerListener,
        Optional<Unmarshaller.Listener> unmarshallerListener,
        Optional<SchemaHandler> schemaHandler
    ) {
        _properties = properties;
        _typeConverter = typeConverter;
        _marshallerListener = marshallerListener;
        _unmarshallerListener = unmarshallerListener;
        _schemaHandler = schemaHandler;
    }

    @Override
    public JSONProvider<?> getService(
        Bundle bundle,
        ServiceRegistration<JSONProvider<?>> registration) {

        return createJsonProvider(_properties);
    }

    @Override
    public void ungetService(
        Bundle bundle,
        ServiceRegistration<JSONProvider<?>> registration,
        JSONProvider<?> service) {

    }
    private final Optional<Marshaller.Listener> _marshallerListener;
    private final Optional<SchemaHandler> _schemaHandler;
    private final Optional<Unmarshaller.Listener> _unmarshallerListener;
    private Dictionary<String, ?> _properties;
    private Optional<TypeConverter> _typeConverter;

    private JSONProvider<?> createJsonProvider(
        Dictionary<String, ?> properties) {

        PropertyWrapper wrapper = new PropertyWrapper(properties);

        JSONProvider<Object> jsonProvider = new JSONProvider<>();

        _typeConverter.ifPresent(jsonProvider::setTypeConverter);
        _marshallerListener.ifPresent(jsonProvider::setMarshallerListener);
        _unmarshallerListener.ifPresent(jsonProvider::setUnmarshallerListener);
        _schemaHandler.ifPresent(jsonProvider::setSchemaHandler);

        wrapper.applyBoolean(
            "drop.root.element", jsonProvider::setDropRootElement);
        wrapper.applyBoolean(
            "attributes.to.elements",
            jsonProvider::setAttributesToElements);
        wrapper.applyBoolean(
            "convert.types.to.strings",
            jsonProvider::setConvertTypesToStrings);
        wrapper.applyBoolean(
            "drop.collection.wrapper.element",
            jsonProvider::setDropCollectionWrapperElement);
        wrapper.applyBoolean(
            "drop.elements.in.xml.stream",
            jsonProvider::setDropElementsInXmlStream);
        wrapper.applyBoolean(
            "enable.buffering", jsonProvider::setEnableBuffering);
        wrapper.applyBoolean(
            "escape.forward.slashes.always",
            jsonProvider::setEscapeForwardSlashesAlways);
        wrapper.applyBoolean(
            "ignore.empty.array.values",
            jsonProvider::setIgnoreEmptyArrayValues);
        wrapper.applyBoolean(
            "ignore.mixed.content",
            jsonProvider::setIgnoreMixedContent);
        wrapper.applyBoolean(
            "ignore.namespaces", jsonProvider::setIgnoreNamespaces);
        wrapper.applyBoolean(
            "read.xsi.type", jsonProvider::setReadXsiType);
        wrapper.applyBoolean(
            "serialize.as.array",
            jsonProvider::setSerializeAsArray);
        wrapper.applyBoolean(
            "support.unwrapped",
            jsonProvider::setSupportUnwrapped);
        wrapper.applyBoolean(
            "write.null.as.string",
            jsonProvider::setWriteNullAsString);
        wrapper.applyBoolean(
            "marshall.as.jaxb.element",
            jsonProvider::setMarshallAsJaxbElement);
        wrapper.applyBoolean(
            "single.jaxb.context",
            jsonProvider::setSingleJaxbContext);
        wrapper.applyBoolean(
            "skip.jaxb.checks",
            jsonProvider::setSkipJaxbChecks);
        wrapper.applyBoolean(
            "unmarshall.as.jaxb.element",
            jsonProvider::setUnmarshallAsJaxbElement);
        wrapper.applyBoolean(
            "use.single.context.for.packages",
            jsonProvider::setUseSingleContextForPackages);
        wrapper.applyBoolean(
            "validate.before.write",
            jsonProvider::setValidateBeforeWrite);
        wrapper.applyBoolean(
            "validate.input",
            jsonProvider::setValidateInput);
        wrapper.applyBoolean(
            "validate.output",
            jsonProvider::setValidateOutput);
        wrapper.applyBoolean(
            "xml.root.as.jaxb.element",
            jsonProvider::setXmlRootAsJaxbElement);
        wrapper.applyBoolean(
            "validate.output",
            jsonProvider::setValidateOutput);
        wrapper.applyBoolean(
            "validate.output",
            jsonProvider::setValidateOutput);
        wrapper.applyBoolean(
            "xml.type.as.jaxb.element.only",
            jsonProvider::setXmlTypeAsJaxbElementOnly);
        wrapper.applyBoolean(
            "write.xsi.type", jsonProvider::setWriteXsiType);

        wrapper.applyString(
            "array.keys",
            s -> jsonProvider.setArrayKeys(
                Arrays.asList(s.split(","))));
        wrapper.applyString(
            "consume.media.types",
            s -> jsonProvider.setArrayKeys(
                Arrays.asList(s.split(","))));
        wrapper.applyString(
            "convention", jsonProvider::setConvention);
        wrapper.applyString(
            "namespace.separator",
            jsonProvider::setNamespaceSeparator);
        wrapper.applyString(
            "produce.media.types",
            s -> jsonProvider.setProduceMediaTypes(
                Arrays.asList(s.split(","))));
        wrapper.applyString(
            "out.drop.elements",
            s -> jsonProvider.setOutDropElements(
                Arrays.asList(s.split(","))));
        wrapper.applyString(
            "schema.locations",
            s -> jsonProvider.setSchemaLocations(
                Arrays.asList(s.split(","))));

        return jsonProvider;
    }
}
