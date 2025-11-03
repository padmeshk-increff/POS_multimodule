package com.increff.pos.factory;

import com.increff.pos.entity.OrderItem;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating OrderItem entities using Instancio.
 */
public final class OrderItemFactory {

    private OrderItemFactory() {
    }

    /**
     * Model for a 'new' OrderItem, not yet saved.
     * ID is null. Quantity and Price are positive. OrderId/ProductId are positive.
     */
    private static final Model<OrderItem> NEW_ITEM_MODEL = Instancio.of(OrderItem.class)
            .set(field(OrderItem::getId), null)
            .generate(field(OrderItem::getOrderId), gen -> gen.ints().min(1))
            .generate(field(OrderItem::getProductId), gen -> gen.ints().min(1))
            .generate(field(OrderItem::getQuantity), gen -> gen.ints().min(1).max(10))
            .generate(field(OrderItem::getSellingPrice), gen -> gen.doubles().min(1.0).max(1000.0))
            .toModel();

    /**
     * Model for a 'persisted' OrderItem, as if from the DB.
     * ID, Quantity, Price, OrderId, ProductId are positive.
     */
    private static final Model<OrderItem> PERSISTED_ITEM_MODEL = Instancio.of(OrderItem.class)
            .generate(field(OrderItem::getId), gen -> gen.ints().min(1))
            .generate(field(OrderItem::getOrderId), gen -> gen.ints().min(1))
            .generate(field(OrderItem::getProductId), gen -> gen.ints().min(1))
            .generate(field(OrderItem::getQuantity), gen -> gen.ints().min(1).max(10))
            .generate(field(OrderItem::getSellingPrice), gen -> gen.doubles().min(1.0).max(1000.0))
            .toModel();

    /**
     * Creates a mock 'new' OrderItem (ID is null).
     * @param orderId The specific Order ID.
     * @param productId The specific Product ID.
     * @return A new OrderItem object.
     */
    public static OrderItem mockNewObject(Integer orderId, Integer productId) {
        return Instancio.of(NEW_ITEM_MODEL)
                .set(field(OrderItem::getOrderId), orderId)
                .set(field(OrderItem::getProductId), productId)
                .create();
    }

    public static OrderItem mockNewObject(Integer orderId, Integer productId,Integer quantity,Double sellingPrice) {
        return Instancio.of(NEW_ITEM_MODEL)
                .set(field(OrderItem::getOrderId), orderId)
                .set(field(OrderItem::getProductId), productId)
                .set(field(OrderItem::getQuantity), quantity)
                .set(field(OrderItem::getSellingPrice), sellingPrice)
                .create();
    }
    /**
     * Creates a mock 'persisted' OrderItem with random data.
     * @return A persisted OrderItem object.
     */
    public static OrderItem mockPersistedObject() {
        return Instancio.of(PERSISTED_ITEM_MODEL)
                .create();
    }

    /**
     * Creates a mock 'persisted' OrderItem with specific IDs.
     * @param id The specific OrderItem ID (PK).
     * @param orderId The specific Order ID (FK).
     * @param productId The specific Product ID (FK).
     * @return A persisted OrderItem object.
     */
    public static OrderItem mockPersistedObject(Integer id, Integer orderId, Integer productId) {
        return Instancio.of(PERSISTED_ITEM_MODEL)
                .set(field(OrderItem::getId), id)
                .set(field(OrderItem::getOrderId), orderId)
                .set(field(OrderItem::getProductId), productId)
                .create();
    }
}