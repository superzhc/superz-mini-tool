package com.github.superzhc.core.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class MapHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MapHelper.class);

    public static <T> T mapToBean(Map<String, ?> map, Class<T> beanClass) {
        if (null == map)
            return null;

        try {
            boolean emptyConstructor = false;
            Constructor[] constructors = beanClass.getDeclaredConstructors();
            for (Constructor constructor : constructors) {
                if (constructor.getParameterCount() == 0) {
                    emptyConstructor = true;
                    break;
                }
            }
            if (!emptyConstructor)
                throw new RuntimeException("无空构造函数，Map无法转" + beanClass.getName());

            T obj = beanClass.newInstance();

            Field[] fields = beanClass.getDeclaredFields();
            for (Field field : fields) {
                int mod = field.getModifiers();
                // 静态变量不做处理，一般Bean中不存在静态变量
                if (Modifier.isStatic(mod))
                    continue;

                field.setAccessible(true);
                field.set(obj, map.get(field.getName()));
            }

            return obj;
        } catch (Exception e) {
            throw new RuntimeException("转化失败", e);
        }
    }

    public static Map<String, ?> beanToMap(Object obj) {
        if (null == obj) return null;

        Map<String, Object> map = new HashMap<>();

        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                int mod = field.getModifiers();
                // 静态变量不做处理，一般Bean中不存在静态变量
                if (Modifier.isStatic(mod))
                    continue;

                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return map;
    }
}
