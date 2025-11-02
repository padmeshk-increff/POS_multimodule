package com.increff.pos.helper;

import com.increff.pos.entity.Invoice;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.model.form.InvoiceItemForm;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class for Invoice-related operations that DEPEND on entity classes.
 * This class is in the server module since entities are now in server.
 * 
 * For entity-independent operations (like file I/O), see InvoiceUtil in pos_commons.
 */
public class InvoiceHelper {

    /**
     * Creates an InvoiceForm from Order, OrderItems, and Product entities.
     * This method depends on entity classes, so it belongs in the server module.
     */
    public static InvoiceForm createInvoiceForm(Order order, List<OrderItem> items, Map<Integer, Product> productMap) {
        InvoiceForm form = new InvoiceForm();
        form.setOrderId(order.getId());
        form.setOrderDate(order.getCreatedAt());
        form.setCustomerName(order.getCustomerName());
        form.setCustomerPhone(order.getCustomerPhone());
        form.setTotalAmount(order.getTotalAmount());

        List<InvoiceItemForm> itemForms = items.stream().map(item -> {
            InvoiceItemForm itemForm = new InvoiceItemForm();
            Product product = productMap.get(item.getProductId());
            itemForm.setProductName(product != null ? product.getName() : "Unknown Product");
            itemForm.setBarcode(product != null ? product.getBarcode() : "N/A");
            itemForm.setQuantity(item.getQuantity());
            itemForm.setMrp(product != null ? product.getMrp() : 0.0);
            itemForm.setSellingPrice(item.getSellingPrice());
            return itemForm;
        }).collect(Collectors.toList());
        form.setItems(itemForms);
        return form;
    }

    /**
     * Creates an Invoice entity with the given order ID and file path.
     * This method depends on the Invoice entity class.
     */
    public static Invoice createInvoiceEntity(Integer orderId, String filePath) {
        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setFilePath(filePath);
        return invoice;
    }

    /**
     * Maps a list of Products by their IDs for quick lookup.
     * This method depends on the Product entity class.
     */
    public static Map<Integer, Product> mapByProductIds(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
