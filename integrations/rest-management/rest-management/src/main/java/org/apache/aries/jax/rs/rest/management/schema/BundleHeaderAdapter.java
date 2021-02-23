package org.apache.aries.jax.rs.rest.management.schema;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BundleHeaderAdapter extends XmlAdapter<BundleHeaderAdapter.BundleHeaderAdapted, BundleHeaderSchema> {

    @XmlRootElement(name = "bundleHeader")
    public static class BundleHeaderAdapted {
        public List<Header> entry = new ArrayList<>();
    }

    public static class Header {
        @XmlAttribute(name = "key", required = true)
        public String key;
        @XmlAttribute(name = "value", required = true)
        public String value;
    }

    @Override
    public BundleHeaderAdapted marshal(BundleHeaderSchema bundleHeaderSchema) throws Exception {
        BundleHeaderAdapted adapter = new BundleHeaderAdapted();
        bundleHeaderSchema.forEach((k, v) -> adapter.entry.add(build(k, v)));
        return adapter;
    }

    @Override
    public BundleHeaderSchema unmarshal(BundleHeaderAdapted adapter) throws Exception {
        BundleHeaderSchema map = new BundleHeaderSchema();
        adapter.entry.forEach(header -> map.put(header.key, header.value));
        return map;
    }

    static Header build(String key, String value) {
        final Header header = new Header();
        header.key = key;
        header.value = value;
        return header;
    }

}
