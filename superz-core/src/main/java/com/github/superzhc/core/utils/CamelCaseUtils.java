package com.github.superzhc.core.utils;

/**
 * 驼峰命名
 *
 * @author superz
 * @create 2022/7/1 17:16
 **/
public class CamelCaseUtils {
    public static String underscore2camelCase(String str) {
        StringBuilder sb = new StringBuilder();

        boolean b = false;
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);
            if (c == '_') {
                b = true;
            } else {
                if (b) {
                    sb.append(Character.toUpperCase(c));
                    b = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static String camelCase2underscore(String str) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i == 0) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append('_').append(Character.toLowerCase(c));
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
