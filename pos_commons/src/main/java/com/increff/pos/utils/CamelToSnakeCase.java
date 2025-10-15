package com.increff.pos.utils;

public class CamelToSnakeCase {
    public static String convert(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        // Append the first character in lowercase
        result.append(Character.toLowerCase(name.charAt(0)));
        // Iterate through the rest of the string
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            // If an uppercase letter is found, prepend it with an underscore
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
