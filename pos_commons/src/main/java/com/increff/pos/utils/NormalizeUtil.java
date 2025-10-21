package com.increff.pos.utils;

import com.increff.pos.model.form.*;

public class NormalizeUtil {

    public static void normalize(ClientForm clientForm) {
        clientForm.setClientName(clientForm.getClientName().trim().toLowerCase());
    }

    public static void normalize(ProductForm productForm){
        productForm.setName(productForm.getName().trim().toLowerCase());
        productForm.setCategory(productForm.getCategory().trim().toLowerCase());
    }

    public static void normalize(OrderForm orderForm){
        orderForm.setCustomerName(orderForm.getCustomerName().trim().toLowerCase());
    }

    public static void normalize(UserForm form) {
        if (form.getEmail() != null) {
            form.setEmail(form.getEmail().trim().toLowerCase());
        }
    }

    public static void normalize(LoginForm loginForm) {
        if (loginForm.getEmail() != null) {
            loginForm.setEmail(loginForm.getEmail().trim().toLowerCase());
        }
    }
}
