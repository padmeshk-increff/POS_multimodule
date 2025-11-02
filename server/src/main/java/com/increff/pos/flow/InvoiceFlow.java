package com.increff.pos.flow;

import com.increff.pos.api.InvoiceApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Invoice;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.helper.InvoiceHelper;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.utils.InvoiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = ApiException.class)
public class InvoiceFlow {

    @Autowired
    private OrderApi orderApi;
    @Autowired
    private OrderItemApi orderItemApi;
    @Autowired
    private ProductApi productApi;
    @Autowired
    private InvoiceApi invoiceApi;

    @Value("${invoice.storage.path}")
    private String invoiceStoragePath;

    public InvoiceForm generateInvoiceForm(Integer orderId) throws ApiException {
        invoiceApi.checkInvoiceDoesNotExist(orderId);
        Order order = orderApi.updateInvoiceOrder(orderId);

        List<OrderItem> items = orderItemApi.getAllByOrderId(orderId);
        List<Integer> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toList());
        List<Product> products = productApi.getByIds(productIds);

        Map<Integer, Product> productMap = InvoiceHelper.mapByProductIds(products);

        return InvoiceHelper.createInvoiceForm(order, items, productMap);
    }

    public void storeInvoiceData(InvoiceData invoiceData) throws ApiException{
        String filePath = InvoiceUtil.savePdfToFile(invoiceData.getBase64Pdf(), invoiceData.getOrderId(), invoiceStoragePath);
        Invoice invoice = InvoiceHelper.createInvoiceEntity(invoiceData.getOrderId(), filePath);
        invoiceApi.insert(invoice);

        orderApi.updateInvoicePathById(invoiceData.getOrderId(), filePath);
    }
}

