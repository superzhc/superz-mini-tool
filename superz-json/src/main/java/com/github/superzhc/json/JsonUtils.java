package com.github.superzhc.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author superz
 * @create 2022/4/2 10:05
 **/
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        //2022年12月6日 提供对Java8 LocalDate、LocalTime、LocalDateTime支持
        // 日期和时间格式化
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        mapper.registerModule(javaTimeModule);

        //允许使用未带引号的字段名
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        //允许使用单引号
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 忽略json字符串中不识别的属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 忽略无法转换的对象
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 日期类型字符串处理
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    }

    private static Pattern pattern = Pattern.compile("^([\\s\\S]+)\\[([0-9]+)\\]$");

    public static ObjectMapper mapper() {
        return mapper;
    }

    public static Object[] convertPaths(String path) {
        List<String> subStrs = new ArrayList<>();

        int len = path.length();
        boolean isEscape = false;
        StringBuilder subStr = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);
            // 未转义
            if (!isEscape) {
                if (c == '\\') {
                    isEscape = true;
                } else if (c == '.') {
                    subStrs.add(subStr.toString());
                    subStr.setLength(0);
                } else {
                    subStr.append(c);
                }
            } else {
                // 前一个字符是转义符，下一个字符还是转义符保持转义状态为真；其他状态下都为false
                if (c == '\\') {
                    // 将上一个非转义含义的字符添加到字串中
                    subStr.append('\\');
                    isEscape = true;
                } else if (c == '.') {
                    subStr.append(c);
                    isEscape = false;
                } else {
                    // 上一个字符是非转义字符的意义，因此也要加上
                    subStr.append('\\').append(c);
                    isEscape = false;
                }
            }
        }

        if (subStr.length() > 0) {
            subStrs.add(subStr.toString());
        }

        List<Object> paths = new ArrayList<>();
        // 对每个子字符串判断是否是数组
        for (String s : subStrs) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                paths.add(matcher.group(1));
                paths.add(Integer.valueOf(matcher.group(2)));
            } else {
                paths.add(s);
            }
        }
        return paths.toArray();
    }

    public static String asString(JsonNode json) {
        return asString((Object) json);
    }

    public static String asString(Map<?, ?> map) {
        return asString((Object) map);
    }

    public static <T> String asString(T[] objs) {
        return asString((Object) objs);
    }

    public static <T> String asString(List<T> lst) {
        return asString((Object) lst);
    }

    public static String asString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String format(String str) {
        JsonNode json = json(str);
        return format(json);
    }

    public static String format(JsonNode json) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String format(Map<?, ?> map) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String format(List<?> lst) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lst);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> map(String json, Object... paths) {
        JsonNode node = loads(json, paths);
        return map(node);
    }

    public static Map<String, Object> map(JsonNode json, Object... paths) {
        JsonNode childNode = object(json, paths);
        return mapper.convertValue(childNode, LinkedHashMap.class);
    }

    /**
     * 推荐使用{@method loads}
     *
     * @param path
     * @param paths
     * @return
     */
    @Deprecated
    public static JsonNode file(String path, Object... paths) {
        return loads(new File(path), paths);
    }

    public static JsonNode loads(InputStream in, Object... paths) {
        try {
            JsonNode node = mapper.readTree(in);
            return object(node, paths);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode loads(File file, Object... paths) {
        try {
            JsonNode node = mapper.readTree(file);
            return object(node, paths);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode loads(String json, Object... paths) {
        try {
            JsonNode node = mapper.readTree(json);
            return object(node, paths);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 推荐使用{@method loads}
     *
     * @param json
     * @param paths
     * @return
     */
    @Deprecated
    public static JsonNode json(String json, Object... paths) {
        return loads(json, paths);
    }

    public static String simpleString(String json, Object... paths) {
        JsonNode childNode = loads(json, paths);
        return text(childNode);
    }

    public static JsonNode object(JsonNode json, Object... paths) {
        JsonNode node = json;
//        for (String path : paths) {
//            if (!path.startsWith("/")) {
//                path = "/" + path;
//            }
//            node = node.at(path);
//        }

        if (null == node) {
            return null;
        }

        for (Object path : paths) {
            if (null == path) {
                continue;
            }

            if (path.getClass() == String.class) {
                String str = (String) path;
                if (!str.startsWith("/")) {
                    str = "/" + str;
                }
                node = node.at(str);
            } else if (path.getClass() == int.class || path.getClass() == Integer.class) {
                node = node.get((int) path);
            } else {
                throw new RuntimeException("json 子节点的获取仅支持字符串字段和整型index序号");
            }
        }
        return node;
    }

    public static ArrayNode array(JsonNode json, Object... paths) {
        JsonNode childNode = object(json, paths);
        return (ArrayNode) childNode;
    }

    @Deprecated
    public static Object object2(JsonNode json, Object... paths) {
        return objectValue(json, paths);
    }

    public static Object objectValue(JsonNode json, Object... paths) {
        JsonNode childNode = object(json, paths);
        if (null == childNode) {
            return null;
        }

        if (childNode.isMissingNode()) {
            return null;
        } else if (childNode.isObject()) {
            return map(childNode);
        } else if (childNode.isArray()) {
            List<Object> lst = list(childNode);
            return lst;
        } else if (childNode.isShort()) {
            return childNode.shortValue();
        } else if (childNode.isInt()) {
            return childNode.intValue();
        } else if (childNode.isLong()) {
            return childNode.longValue();
        } else if (childNode.isFloat()) {
            return childNode.floatValue();
        } else if (childNode.isDouble()) {
            return childNode.doubleValue();
        } else if (childNode.isBigDecimal()) {
            return childNode.decimalValue();
        } else if (childNode.isBigInteger()) {
            return childNode.bigIntegerValue();
        } else if (childNode.isBoolean()) {
            return childNode.booleanValue();
        } else if (childNode.isBinary()) {
            try {
                return childNode.binaryValue();
            } catch (Exception e) {
                log.error("转换binary异常！", e);
                return null;
            }
        } else if (childNode.isTextual()) {
            return childNode.textValue();
        } else if (childNode.isNull()) {
            return null;
        } else {
            log.debug("数据【{}】未知类型", asString(childNode));
            return null;
        }
    }

    /**
     * 见 simpleString 函数
     *
     * @param json
     * @param paths
     * @return
     */
    @Deprecated
    public static String string(String json, Object... paths) {
        return simpleString(json, paths);
    }

    public static String string(JsonNode node, Object... paths) {
        JsonNode childNode = object(node, paths);
        return null == childNode ? null : childNode.asText();
    }

    public static String text(JsonNode node, Object... paths) {
        JsonNode childNode = object(node, paths);
        if (null == childNode) {
            return null;
        } else if (childNode.isObject() || childNode.isArray()) {
            return asString(childNode);
        } else {
            return string(childNode);
        }
    }

    public static Integer integer(JsonNode node, Object... paths) {
        JsonNode childNode = object(node, paths);
        return null == childNode ? null : childNode.asInt();
    }

    public static Double aDouble(JsonNode node, Object... paths) {
        JsonNode childNode = object(node, paths);
        return null == childNode ? null : childNode.asDouble();
    }

    public static Long aLong(JsonNode node, Object... paths) {
        JsonNode childNode = object(node, paths);
        return null == childNode ? null : childNode.asLong();
    }

    public static Boolean bool(JsonNode node, Object... paths) {
        JsonNode childNode = object(node, paths);
        return null == childNode ? Boolean.FALSE : childNode.asBoolean();
    }

    public static Date date(JsonNode node, String format, Object... paths) {
        JsonNode childNode = object(node, paths);
        if (null == childNode) {
            return null;
        }

        String text = childNode.asText();
        try {
            return new SimpleDateFormat(format).parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDateTime localDateTime(JsonNode node, String format, Object... paths) {
        JsonNode childNode = object(node, paths);
        if (null == childNode) {
            return null;
        }

        String text = childNode.asText();

        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(format));
    }

    public static <T> List<T> list(JsonNode node, Object... paths) {
        ArrayNode arrayNode = array(node, paths);
        List<T> lst = new ArrayList<>(arrayNode.size());
        for (int i = 0, len = arrayNode.size(); i < len; i++) {
            JsonNode arrayChildNode = node.get(i);
            T value = (T) objectValue(arrayChildNode);
            lst.add(value);
        }
        return lst;
    }

    public static List<String> stringArray2List(JsonNode node, Object... paths) {
        return Arrays.asList(stringArray(node, paths));
    }

    @Deprecated
    public static String[] stringArray(String json, Object... paths) {
        return stringArray(loads(json), paths);
    }

    public static String[] stringArray(JsonNode node, Object... paths) {
        ArrayNode childNode = array(node, paths);
        String[] arr = new String[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = null == childNode.get(i) ? null : childNode.get(i).asText();
        }
        return arr;
    }

    public static int[] intArray(JsonNode node, Object... paths) {
        ArrayNode childNode = array(node, paths);
        int[] arr = new int[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = null == childNode.get(i) ? null : childNode.get(i).asInt();
        }
        return arr;
    }


    public static long[] longArray(JsonNode node, Object... paths) {
        ArrayNode childNode = array(node, paths);
        long[] arr = new long[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = null == childNode.get(i) ? null : childNode.get(i).asLong();
        }
        return arr;
    }

    public static LocalDateTime[] long2DateTimeArray(JsonNode node, Object... paths) {
        ArrayNode childNode = array(node, paths);
        LocalDateTime[] arr = new LocalDateTime[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = null == childNode.get(i) ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(childNode.get(i).asLong()), ZoneId.systemDefault());
        }
        return arr;
    }

    public static double[] doubleArray(JsonNode node, Object... paths) {
        ArrayNode childNode = array(node, paths);
        double[] arr = new double[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = null == childNode.get(i) ? null : childNode.get(i).asDouble();
        }
        return arr;
    }

    public static Date[] dateArray(JsonNode node, String format, Object... paths) {
        ArrayNode childNode = array(node, paths);
        Date[] arr = new Date[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = date(childNode.get(i), format);
        }
        return arr;
    }

    public static LocalDateTime[] localDateTimeArray(JsonNode node, String format, Object... paths) {
        ArrayNode childNode = array(node, paths);
        LocalDateTime[] arr = new LocalDateTime[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            arr[i] = localDateTime(childNode.get(i), format);
        }
        return arr;
    }

    public static Map<String, Object>[] newObjectArray(JsonNode node, Object... paths) {
        return newObjectArray(node, paths, (List<String>) null);
    }

    public static Map<String, Object>[] newObjectArray4Keys(JsonNode node, List<String> keys) {
        return newObjectArray(node, null, keys);
    }

    public static Map<String, Object>[] newObjectArray4Keys(JsonNode node, String... keys) {
        return newObjectArray(node, null, keys);
    }

    public static Map<String, Object>[] newObjectArray(JsonNode node, Object[] paths, String... keys) {
        return newObjectArray(node, paths, Arrays.asList(keys));
    }

    public static Map<String, Object>[] newObjectArray(JsonNode node, Object[] paths, List<String> keys) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        Map<String, Object>[] arr = new Map[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            JsonNode item = childNode.get(i);
            if (null == item) {
                continue;
            }

            Map<String, Object> map = new LinkedHashMap<>();
            if (null == keys || keys.size() == 0) {
                keys = null == keys ? new ArrayList<>() : keys;
                Iterator<String> fieldNames = item.fieldNames();
                while (fieldNames.hasNext()) {
                    keys.add(fieldNames.next());
                }
            }

            for (String key : keys) {
                map.put(key, objectValue(item, key));
            }

            arr[i] = map;
        }
        return arr;
    }

    public static String[] mapOneArray(JsonNode node, String key, String... paths) {
        ArrayNode childNode = array(node, paths);
        String[] arr = new String[childNode.size()];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            JsonNode item = childNode.get(i);
            if (null == item) {
                continue;
            }

            arr[i] = text(item, key);
        }
        return arr;
    }

    /**
     * Deprecated，推荐使用 newObjectArray
     *
     * @param node
     * @param keys
     * @param paths
     * @return
     */
    @Deprecated
    public static Map<String, String>[] objectArray2Map(JsonNode node, String[] keys, String... paths) {
        return objectArray2Map(node, Arrays.asList(keys), paths);
    }

    /**
     * Deprecated，推荐使用 newObjectArray
     *
     * @param node
     * @param keys
     * @param paths
     * @return
     */
    @Deprecated
    public static Map<String, String>[] objectArray2Map(JsonNode node, List<String> keys, String... paths) {
        Map<String, Object>[] originData = newObjectArray(node, paths, keys);
        Map<String, String>[] data = new Map[originData.length];
        for (int i = 0, len = originData.length; i < len; i++) {
            Map<String, Object> originItem = originData[i];
            Map<String, String> item = new LinkedHashMap<>();
            for (Map.Entry<String, Object> originEntry : originItem.entrySet()) {
                item.put(originEntry.getKey(), String.valueOf(originEntry.getValue()));
            }
            data[i] = item;
        }
        return data;
    }

    /**
     * Deprecated，推荐使用 newObjectArray
     *
     * @param node
     * @param paths
     * @return
     */
    @Deprecated
    public static Map<String, String>[] objectArray2Map(JsonNode node, String... paths) {
        return objectArray2Map(node, (List<String>) null, paths);
    }

    public static Object[][] newArrayArray(JsonNode node, String... paths) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        Object[][] arr = new Object[childNode.size()][];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            JsonNode item = childNode.get(i);
            if (null == item) {
                continue;
            }

            ArrayNode arrayNode = array(item);
            Object[] objArr = new Object[arrayNode.size()];
            for (int j = 0, arrLen = arrayNode.size(); j < arrLen; j++) {
                objArr[j] = objectValue(arrayNode.get(j));
            }

            arr[i] = objArr;
        }
        return arr;
    }

    public static Map<String, Object>[] arrayArray2Map(JsonNode node, String... keys) {
        return arrayArray2Map(node, null, keys);
    }

    public static Map<String, Object>[] arrayArray2Map(JsonNode node, String[] paths, String... keys) {
        Object[][] originData = newArrayArray(node, paths);
        Map<String, Object>[] data = new Map[originData.length];
        for (int i = 0, len = originData.length; i < len; i++) {
            Object[] originItem = originData[i];
            if (null == originItem) {
                data[i] = null;
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            int indexCursor = 1;
            int keySize = null == keys ? 0 : keys.length;
            for (int j = 0, itemLen = originItem.length; j < itemLen; j++) {
                Object value = originItem[j];
                String key;
                if (j < keySize) {
                    key = keys[j];
                } else {
                    key = String.format("_%d", indexCursor);
                    indexCursor++;
                }
                item.put(key, value);
            }
            data[i] = item;
        }
        return data;
    }

    public static String[][] arrayArray(JsonNode node, String... paths) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        String[][] arr = new String[childNode.size()][];
        for (int i = 0, len = childNode.size(); i < len; i++) {
            JsonNode item = childNode.get(i);
            if (null == item) {
                continue;
            }

            arr[i] = stringArray(item);
        }
        return arr;
    }

    public static String[] objectOneArray(JsonNode node, String key, String... paths) {
        return mapOneArray(node, key, paths);
    }

    public static String[] objectKeys(JsonNode node, String... paths) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        List<String> keys = new ArrayList<>();
        Iterator<String> fieldNames = childNode.fieldNames();
        while (fieldNames.hasNext()) {
            keys.add(fieldNames.next());
        }
        return keys.toArray(new String[keys.size()]);
    }

    public static String[] objectArrayKeys(JsonNode node, String... childPaths) {
        return objectArrayKeys(node, null, childPaths);
    }

    public static String[] objectArrayKeys(JsonNode node, String[] paths, String[] childPaths) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        // 保证列的顺序
        Set<String> columnNames = new LinkedHashSet<>();
//        if (childNode.isArray()) {
        for (JsonNode item : childNode) {
            JsonNode childItem = item;
            if (null != childPaths) {
                childItem = object(item, childPaths);
            }
            Iterator<String> fieldNames = childItem.fieldNames();
            while (fieldNames.hasNext()) {
                columnNames.add(fieldNames.next());
            }
        }
//        } else if (childNode.isObject()) {
//            JsonNode childNode2 = childNode;
//            if (null != childPaths) {
//                childNode2 = json(childNode, childPaths);
//            }
//            Iterator<String> fieldNames = childNode2.fieldNames();
//            while (fieldNames.hasNext()) {
//                columnNames.add(fieldNames.next());
//            }
//        } else {
//            return null;
//        }
        return columnNames.toArray(new String[columnNames.size()]);
    }

    public static List<String[]> objectArray(JsonNode node, String... childPaths) {
        return objectArray(node, null, childPaths);
    }

    public static List<String[]> objectArray(JsonNode node, String[] paths, String[] childPaths) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        List<String[]> table = new LinkedList<>();

        String[] columnNames = objectArrayKeys(childNode, childPaths);
        // 第一行是列的元数据信息
        table.add(columnNames);

//        if (childNode.isArray()) {
        int columnLength = columnNames.length;
        for (JsonNode item : childNode) {
            JsonNode childItem = item;
            if (null != childPaths) {
                childItem = object(item, childPaths);
            }
            String[] dataRow = new String[columnLength];
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnNames[i];
                JsonNode dataCell = object(childItem, columnName);
                dataRow[i] = text(dataCell);
            }
            table.add(dataRow);
        }
//        } else if (childNode.isObject()) {
//            int columnLength = columnNames.length;
//              JsonNode childNode2=childNode;
//              if(null!=childPaths){
//                  childNode2=json(childNode,childPaths);
//              }
//            String[] dataRow = new String[columnLength];
//            for (int i = 0; i < columnLength; i++) {
//                String columnName = columnNames[i];
//                JsonNode dataCell = json(childNode, columnName);
//                dataRow[i] = text(dataCell);
//            }
//            table.add(dataRow);
//        } else {
//            return null;
//        }
        return table;
    }

    public static List<String[]> objectArrayWithKeys(JsonNode node, List<String> columnNames, String... childPaths) {
        return objectArrayWithKeys(node, null, childPaths, columnNames.toArray(new String[columnNames.size()]));
    }

    public static List<String[]> objectArrayWithKeys(JsonNode node, String[] columnNames, String... childPaths) {
        return objectArrayWithKeys(node, null, childPaths, columnNames);
    }

    public static List<String[]> objectArrayWithKeys(JsonNode node, String[] paths, String[] childPaths, String[] columnNames) {
        JsonNode childNode = node;
        if (null != paths) {
            childNode = object(node, paths);
        }

        List<String[]> table = new LinkedList<>();
        table.add(columnNames);
        int columnLength = columnNames.length;
        for (JsonNode item : childNode) {
            JsonNode childItem = item;
            if (null != childPaths) {
                childItem = object(item, childPaths);
            }
            String[] dataRow = new String[columnLength];
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnNames[i];
                JsonNode dataCell = object(childItem, columnName);
                dataRow[i] = text(dataCell);
            }
            table.add(dataRow);
        }
        return table;
    }

    public static List<String[]> arrayArray2(JsonNode node, String... paths) {
        return Arrays.asList(arrayArray(node, paths));
    }

    /**
     * 推荐使用 objectArrayKeys
     *
     * @param datas
     * @param childPaths
     * @return
     */
    @Deprecated
    public static List<String> extractObjectColumnName(JsonNode datas, String... childPaths) {
        return Arrays.asList(objectArrayKeys(datas, childPaths));
    }

    /**
     * 推荐使用 objectArray
     *
     * @param datas
     * @param childPaths
     * @return
     */
    @Deprecated
    public static List<String[]> extractObjectData(JsonNode datas, String... childPaths) {
        List<String[]> table = objectArray(datas, null, childPaths);
        return table;
    }

    /**
     * 推荐使用 objectArrayWithKeys
     *
     * @param datas
     * @param columnNames
     * @param childPaths
     * @return
     */
    @Deprecated
    public static List<String[]> extractObjectData(JsonNode datas, List<String> columnNames, String... childPaths) {
        return objectArrayWithKeys(datas, columnNames.toArray(new String[columnNames.size()]), childPaths);
    }

    //region===================================写节点===================================================================
    public static JsonNode putArray(ObjectNode node0, String arrayName, Object value, String... children) {
        ObjectNode node = node0;
        if (null != children && children.length > 0) {
            for (String child : children) {
                if (node.hasNonNull(child)) {
                    node = (ObjectNode) node.get(child);
                } else {
                    node = node.putObject(child);
                }
            }
        }

        ArrayNode arrayNode;
        if (node.has(arrayName)) {
            arrayNode = (ArrayNode) node.get(arrayName);
        } else {
            arrayNode = node.putArray(arrayName);
        }

        putArray(arrayNode, value);

        return node0;
    }

    public static JsonNode putArray(ArrayNode node0, Object value) {
        if (null == value) {
            node0.addNull();
        } else {
            node0.add(loads(asString(value)));
        }
        return node0;
    }

    /**
     * 支持多层嵌套对象节点的键值创建
     *
     * @param node0
     * @param key
     * @param value
     * @param children
     * @return
     */
    public static JsonNode put(ObjectNode node0, String key, Object value, String... children) {
        ObjectNode node = node0;
        if (null != children && children.length > 0) {
            for (String child : children) {
                if (node.hasNonNull(child)) {
                    node = (ObjectNode) node.get(child);
                } else {
//                    ObjectNode childNode = mapper().createObjectNode();
//                    node.put(child, childNode);
//                    node = childNode;
                    node = node.putObject(child);
                }
            }
        }

        if (null == value) {
//            node.set(key, null);
            node.putNull(key);
        }
//        else if (value.getClass().isArray()) {
//            Object array = value;
//            ArrayNode arrayNode = node.putArray(key);
//
//            if (array instanceof int[])
//                for (int item : (int[]) array) arrayNode.add(item);
//            else if (array instanceof boolean[])
//                for (boolean item : (boolean[]) array) arrayNode.add(item);
//            else if (array instanceof long[])
//                for (long item : (long[]) array) arrayNode.add(item);
//            else if (array instanceof float[])
//                for (float item : (float[]) array) arrayNode.add(item);
//            else if (array instanceof double[])
//                for (double item : (double[]) array) arrayNode.add(item);
//            else if (array instanceof short[])
//                for (short item : (short[]) array) arrayNode.add(item);
//            else if (array instanceof byte[])
//                for (byte item : (byte[]) array) arrayNode.add(item);
//            else if (array instanceof char[])
//                for (char item : (char[]) array) arrayNode.add(item);
//            else {
//                // for(Object item:(Object[]) array)
//            }
//        } else if (value instanceof List) {
//
//        } else if (value instanceof Map) {
//
//        } else {
//            Class<?> valueClass = value.getClass();
//            if (boolean.class == valueClass || valueClass == Boolean.class) {
//                node.put(key, (boolean) value);
//            } else if (int.class == valueClass || valueClass == Integer.class) {
//                node.put(key, (int) value);
//            } else if (short.class == valueClass || valueClass == Short.class) {
//                node.put(key, (short) value);
//            } else if (long.class == valueClass || valueClass == Long.class) {
//                node.put(key, (long) value);
//            } else if (float.class == valueClass || valueClass == Float.class) {
//                node.put(key, (float) value);
//            } else if (double.class == valueClass || valueClass == Double.class) {
//                node.put(key, (double) value);
//            } else if (valueClass == BigDecimal.class) {
//                node.put(key, (BigDecimal) value);
//            } else if (valueClass == BigInteger.class) {
//                node.put(key, (BigInteger) value);
//            } else {
//                node.put(key, value.toString());
//            }
//        }
        else {
            node.set(key, loads(asString(value)));
        }
        return node0;
    }
    //endregion================================写节点===================================================================

    //=============================【实验性】JsonPath支持，若未引入jsonpath依赖包，无法使用如下方法===========================
//    public static JsonNode jsonPath(JsonNode node, String path) {
//        return JsonPathUtils.read(node, path);
//    }
    //=============================【实验性】JsonPath支持===================================================================
}

