package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

@Getter
@Setter
public class ProductForm {

    @NotBlank(message = "Barcode cannot be blank")
    @Size(min = 5, max = 20, message = "Barcode must be between 5 and 20 characters")
    private String barcode;

    @NotBlank(message = "Product name cannot be blank")
    @Size(max = 255, message = "Product name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Category cannot be blank")
    @Size(max = 255, message = "Category cannot exceed 50 characters")
    private String category;

    @NotNull(message = "MRP cannot be null")
    @Positive(message = "MRP must be a positive value")
    @Max(value = 100000, message = "MRP seems too high (cannot exceed 1,00,000)")
    private Double mrp;

    @URL(message = "Image url must be a valid url")
    private String imageUrl;

    @NotNull(message = "Client ID cannot be null")
    private Integer clientId;

}
