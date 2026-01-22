package net.statemesh.k8s.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.statemesh.service.dto.ContainerDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ObjectUtil {
    public static <T> T safeGet(Object object, Supplier<T> defaultValue) {
        return object != null ? defaultValue.get() : null;
    }

    public static <T> T clone(T object, Class<T> objectClass, ObjectMapper objectMapper) {
        return objectMapper.convertValue(object, objectClass);
    }

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2) {
        if (map1 == null && map2 == null) {
            return Map.of();
        } else if (map1 == null) {
            return map2;
        } else if (map2 == null) {
            return map1;
        } else {
            Map<K, V> mergedMap = new HashMap<>(map1);
            mergedMap.putAll(map2);
            return mergedMap;
        }
    }
}
