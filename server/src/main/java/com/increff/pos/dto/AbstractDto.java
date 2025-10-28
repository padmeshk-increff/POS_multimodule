package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AbstractDto {

    protected void normalize(Object form, List<String> exceptions) throws ApiException{
        if(form == null){
            return;
        }

        Set<String> exceptionSet;

        if (exceptions == null || exceptions.isEmpty()) {
            exceptionSet = Collections.emptySet();
        } else {
            exceptionSet = new HashSet<>(exceptions);
        }

        Class<?> clazz = form.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            try {
                if (field.getType() == String.class) {
                    if (exceptionSet.contains(field.getName())) {
                        continue;
                    }
                    field.setAccessible(true);
                    String value = (String) field.get(form);
                    if (value != null) {
                        value = value.trim().toLowerCase();

                        field.set(form, value);
                    }
                    field.setAccessible(false);
                }
            } catch (IllegalAccessException e) {
                throw new ApiException("Error normalizing field: " + field.getName() + " - " + e.getMessage());
            }
        }
    }
}
