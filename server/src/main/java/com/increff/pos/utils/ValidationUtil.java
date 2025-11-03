package com.increff.pos.utils;

import com.increff.pos.commons.exception.FormValidationException;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import javax.validation.Path;

public final class ValidationUtil {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    private ValidationUtil() {
    }

    /**
     * Manually validates a form object and throws a FormValidationException
     * if any errors are found.
     *
     * @param form The form object to validate (e.g., ClientForm)
     * @param <T>  The type of the form
     * @throws FormValidationException if validation fails
     */
    public static <T> void validate(T form) throws FormValidationException {
        Set<ConstraintViolation<T>> violations = validator.validate(form);

        if (!violations.isEmpty()) {
            // Convert the set of violations into a Map<String, String>
            // This is exactly what the old @Valid handler did.
            Map<String, String> errors = violations.stream()
                    .collect(Collectors.toMap(
                            violation -> getFieldName(violation.getPropertyPath()),
                            ConstraintViolation::getMessage,
                            (message1, message2) -> message1 + ", " + message2
                    ));

            // Throw our new custom exception containing the map
            throw new FormValidationException(errors);
        }
    }

    // Helper to get the field name from the violation path
    private static String getFieldName(Path propertyPath) {
        String pathStr = propertyPath.toString();
        // This handles nested paths, but for simple forms, it just returns the field name
        return pathStr.substring(pathStr.lastIndexOf('.') + 1);
    }
}