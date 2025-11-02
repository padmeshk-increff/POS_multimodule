package com.increff.pos.factory;

import com.increff.pos.entity.Order;
import com.increff.pos.model.enums.OrderStatus;
import org.instancio.Instancio;
import org.instancio.Model;

import java.time.ZonedDateTime;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating Order entities using Instancio.
 */
public final class OrderFactory {

    private OrderFactory() {
    }

    /**
     * Model for a 'new' Order, not yet saved.
     * ID is null, Status is CREATED, Amount is 0.0.
     */
    private static final Model<Order> NEW_ORDER_MODEL = Instancio.of(Order.class)
            .set(field(Order::getId), null)
            .set(field(Order::getOrderStatus), OrderStatus.CREATED)
            .set(field(Order::getTotalAmount), 0.0)
            .set(field(Order::getInvoicePath), null)
            .set(field(Order::getCreatedAt), ZonedDateTime.now())
            .toModel();

    /**
     * Model for a 'persisted' Order, as if from the DB.
     * ID is generated, Status is CREATED, Amount is positive.
     */
    private static final Model<Order> PERSISTED_ORDER_MODEL = Instancio.of(Order.class)
            .generate(field(Order::getId), gen -> gen.ints().min(1))
            .set(field(Order::getOrderStatus), OrderStatus.CREATED)
            .generate(field(Order::getTotalAmount), gen -> gen.doubles().min(1.0))
            .set(field(Order::getCreatedAt), ZonedDateTime.now())
            .toModel();

    /**
     * Creates a mock 'new' Order object (ID is null, status is CREATED).
     * @return A new Order object.
     */
    public static Order mockNewObject() {
        return Instancio.of(NEW_ORDER_MODEL)
                .create();
    }

    /**
     * Creates a mock 'persisted' Order object.
     * @return A persisted Order object with a random ID and CREATED status.
     */
    public static Order mockPersistedObject() {
        return Instancio.of(PERSISTED_ORDER_MODEL)
                .create();
    }

    /**
     * Creates a mock 'persisted' Order object with a specific ID and Status.
     * @param id The ID to set.
     * @param status The OrderStatus to set.
     * @return A persisted Order object.
     */
    public static Order mockPersistedObject(Integer id, OrderStatus status) {
        return Instancio.of(PERSISTED_ORDER_MODEL)
                .set(field(Order::getId), id)
                .set(field(Order::getOrderStatus), status)
                .create();
    }

    /**
     * Creates a mock 'persisted' Order object with a specific ID, Status, and Amount.
     * @param id The ID to set.
     * @param status The OrderStatus to set.
     * @param amount The total amount to set.
     * @return A persisted Order object.
     */
    public static Order mockPersistedObject(Integer id, OrderStatus status, Double amount) {
        return Instancio.of(PERSISTED_ORDER_MODEL)
                .set(field(Order::getId), id)
                .set(field(Order::getOrderStatus), status)
                .set(field(Order::getTotalAmount), amount)
                .create();
    }
}