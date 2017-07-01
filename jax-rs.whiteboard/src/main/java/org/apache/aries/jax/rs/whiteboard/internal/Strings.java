package org.apache.aries.jax.rs.whiteboard.internal;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Strings {

    @SuppressWarnings({"unchecked" })
    public static Optional<List<String>> stringPlus(Object obj) {
        if (obj == null) {
            return Optional.empty();
        }

        List<String> strings = new ArrayList<>();

        if (obj instanceof String) {
            strings.add((String)obj);
        }
        else if (Collection.class.isInstance(obj) &&
                (ParameterizedType.class.isAssignableFrom(obj.getClass())) &&
                ((ParameterizedType)obj).getActualTypeArguments()[0].equals(String.class)) {

            for (String item : (Collection<String>)obj) {
                strings.add(item);
            }
        }
        else if (obj.getClass().isArray() &&
                String.class.isAssignableFrom(obj.getClass().getComponentType())) {

            for (String item : (String[])obj) {
                strings.add(item);
            }
        }
        else {
            return Optional.empty();
        }

        return Optional.of(strings);
    }

}