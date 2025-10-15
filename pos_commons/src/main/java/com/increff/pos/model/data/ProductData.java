package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductData {

    private Integer id;
    private String barcode;
    private String name;
    private String category;
    private Double mrp;
    private String imageUrl;
    private Integer clientId;
    private String clientName;
    private Integer quantity;

}
