package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class ClientForm {

    @NotBlank(message = "Client name cannot be empty!")
    @Size(max=255,message = "Client name cannot exceed 255 characters")
    private String clientName;

}
