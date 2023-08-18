package com.github.superzhc.core.utils;

import java.util.*;

public class PrintUtils {


//    public static <T> String print(String[] headers, List<T>[] data) {
//        if (null == data) {
//            return "暂无数据";
//        }
//
//        if (null == headers) {
//            int headerLength = 0;
//            for (List<T> item : data) {
//                headerLength = Math.max(headerLength, item.size());
//            }
//
//            headers = new ArrayList<>();
//            for (int i = 0; i < headerLength; i++) {
//                headers.add(String.format("C%d", i));
//            }
//        }
//
//        int columnSize = headers.size();
//        int[] columnMaxLengths = new int[columnSize];
//        String[] headerRow = new String[columnSize];
//        for (int i = 0; i < columnSize; i++) {
//            columnMaxLengths[i] = stringLength(headers.get(i));
//            headerRow[i] = headers.get(i);
//        }
//
//        List<String[]> rows = new ArrayList<>();
//        for (int j = 0, rowNum = data.size(); j < rowNum; j++) {
//            List<T> item = data.get(j);
//            String[] row = new String[columnSize];
//            for (int m = 0, len = Math.min(columnSize, item.size()); m < len; m++) {
//                String value = String.valueOf(item.get(m));
//                row[m] = value;
//                columnMaxLengths[m] = Math.max(columnMaxLengths[m], (null == value ? 0 : stringLength(value)));
//            }
//            rows.add(row);
//        }
//
//        StringBuilder result = new StringBuilder();
//        result.append(printSeparator(columnMaxLengths)).append("\n");
//        result.append(printRow(headerRow, columnMaxLengths)).append("\n");
//        result.append(printSeparator(columnMaxLengths)).append("\n");
//        for (String[] row : rows) {
//            result.append(printRow(row, columnMaxLengths)).append("\n");
//        }
//        result.append(printSeparator(columnMaxLengths)).append("\n");
//
//        result.append("展示数据条数：").append(data.size()).append("\n");
//
//        return result.toString();
//    }

    //    public static void show(Map<String, ?>... maps) {
//        System.out.println(print(maps));
//    }
//
    public static <T> void print(List<Map<String, T>> maps) {
        print(maps, null == maps ? 0 : maps.size());
    }

    /**
     * 打印所有数据
     *
     * @param maps
     * @return
     */
    public static <T> void print(List<Map<String, T>> maps, int limit) {
        if (null == maps) {
            print((Map<String, T>[]) null, limit);
            return;
        }

        Map<String, T>[] arr = new Map[maps.size()];
        print(maps.toArray(arr));
    }

    public static <T> void print(Map<String, T>... maps) {
        print(maps, null == maps ? 0 : maps.length);
    }

    /**
     * 打印指定条数的数据
     *
     * @param maps
     * @param limit
     * @return
     */
    public static <T> void print(Map<String, T>[] maps, int limit) {
        String out;
        if (null == maps || maps.length == 0) {
            out = "暂无数据";
        }

        // 获取所有的key
        Set<String> keys = new LinkedHashSet<>();
        for (Map<String, ?> map : maps) {
            keys.addAll(map.keySet());
        }

        int[] columnMaxLengths = new int[keys.size()];
        String[] headerRow = new String[keys.size()];
        int i = 0;
        for (String key : keys) {
            columnMaxLengths[i] = stringLength(key);
            headerRow[i] = key;
            i++;
        }

        List<String[]> rows = new ArrayList<>();
        for (int k = 0, mapsLength = maps.length; k < mapsLength; k++) {
            if (k >= limit) {
                break;
            }

            Map<String, ?> map = maps[k];
            String[] row = new String[keys.size()];
            int j = 0;
            for (String key : keys) {
                String value = (null == map || !map.containsKey(key) || null == map.get(key)) ? null : String.valueOf(map.get(key));
                row[j] = value;
                columnMaxLengths[j] = Math.max(columnMaxLengths[j], (null == value ? 0 : stringLength(value)));
                j++;
            }
            rows.add(row);
        }

        StringBuilder result = new StringBuilder();
        result.append(printSeparator(columnMaxLengths)).append("\n");
        result.append(printRow(headerRow, columnMaxLengths)).append("\n");
        result.append(printSeparator(columnMaxLengths)).append("\n");
        for (String[] row : rows) {
            result.append(printRow(row, columnMaxLengths)).append("\n");
        }
        result.append(printSeparator(columnMaxLengths)).append("\n");

        result.append("展示数据条数：" + limit + "，数据总数：").append(maps.length).append("\n");

        out = result.toString();
        System.out.println(out);
    }

    private static String printRow(String[] row, int[] columnMaxLengths) {
        StringBuilder sb = new StringBuilder();
        int columnCount = row.length;
        for (int i = 0; i < columnCount; i++) {
            sb.append("|");
            sb.append(rightPad(row[i], columnMaxLengths[i]));
        }
        sb.append("|");
        return sb.toString();
    }

    private static String printSeparator(int[] columnMaxLengths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnMaxLengths.length; i++) {
            sb.append("+");
            for (int j = 0; j < columnMaxLengths[i] + 1; j++) {
                sb.append("-");
            }
        }
        sb.append("+");
        return sb.toString();
    }

    private static String rightPad(String str, int maxLength) {
        int len = stringLength(str);
        StringBuilder sb = new StringBuilder(null == str ? "" : str);
        for (int i = 0; i < ((maxLength - len) + 1); i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static int stringLength(String str) {
        if (null == str) {
            return 0;
        }

        // String chinese = "[\u0391-\uFFE5]";//匹配中文字符的正则表达式： [\u4e00-\u9fa5]
        // String doubleChar = "[^\\x00-\\xff]";// 匹配双字节字符(包括汉字在内)：[^\x00-\xff]
        String doubleChar = "[" +
                "\u1100-\u115F" +
                "\u2E80-\uA4CF" +
                "\uAC00-\uD7A3" +
                "\uF900-\uFAFF" +
                "\uFE10-\uFE19" +
                "\uFE30-\uFE6F" +
                "\uFF00-\uFF60" +
                "\uFFE0-\uFFE6" +
                "]";

        int valueLength = 0;
        /* 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1 */
        for (int i = 0; i < str.length(); i++) {
            /* 获取一个字符 */
            String temp = str.substring(i, i + 1);
            /* 判断是否为中文字符 */
            if (temp.matches(doubleChar)) {
                /* 中文字符长度为2 */
                valueLength += 2;
            } else {
                /* 其他字符长度为1 */
                valueLength += 1;
            }
        }
        return valueLength;
    }
}
