package com.increff.pos.commons.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class FormValidationException extends ApiException {

    private final Map<String, String> errors;

    public FormValidationException(Map<String, String> errors) {
        super("Form validation failed");
        this.errors = errors;
    }
}