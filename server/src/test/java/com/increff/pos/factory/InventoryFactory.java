package com.increff.pos.factory;

import com.increff.pos.entity.Inventory;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating Inventory entities using Instancio.
 * 'id' (Inventory PK) and 'productId' (FK) are generated as independent
 * random positive integers, unless specified.
 */
public final class InventoryFactory {

    private InventoryFactory() {
    }

    /**
     * Model for a 'new' Inventory item (e.g., just initialized).
     * ID is null. Quantity is 0. ProductID is a random positive integer.
     */
    private static final Model<Inventory> NEW_INVENTORY_MODEL = Instancio.of(Inventory.class)
            .set(field(Inventory::getId), null)
            .set(field(Inventory::getQuantity), 0)
            .generate(field(Inventory::getProductId), gen -> gen.ints().min(1))
            .toModel();

    /**
     * Model for a 'persisted' Inventory item with stock.
     * Quantity, ID, and ProductID are all random positive integers.
     */
    private static final Model<Inventory> PERSISTED_INVENTORY_MODEL = Instancio.of(Inventory.class)
            .generate(field(Inventory::getQuantity), gen -> gen.ints().min(1).max(1000))
            .generate(field(Inventory::getId), gen -> gen.ints().min(1))
            .generate(field(Inventory::getProductId), gen -> gen.ints().min(1))
            .toModel();

    /**
     * Creates a mock Inventory for a new product (quantity 0)
     * with a specific Product ID.
     * The Inventory's own 'id' will be random.
     *
     * @param productId The specific product ID (foreign key) to set.
     * @return An Inventory object with quantity 0 and the specified productId.
     */
    public static Inventory mockNewObject(Integer productId) {
        return Instancio.of(NEW_INVENTORY_MODEL)
                .set(field(Inventory::getProductId), productId)
                .create();
    }

    /**
     * Creates a mock persisted Inventory with random stock,
     * random ID, and random Product ID.
     *
     * @return An Inventory object with random data.
     */
    public static Inventory mockPersistedObject() {
        return Instancio.of(PERSISTED_INVENTORY_MODEL)
                .create();
    }

    /**
     * Creates a mock persisted Inventory with specific stock
     * for a specific Product ID.
     * The Inventory's own 'id' will be random.
     *
     * @param productId The specific product ID (foreign key) to set.
     * @param quantity  The quantity to set.
     * @return An Inventory object with the specified data.
     */
    public static Inventory mockPersistedObject(Integer productId, Integer quantity) {
        return Instancio.of(PERSISTED_INVENTORY_MODEL)
                .set(field(Inventory::getProductId), productId)
                .set(field(Inventory::getQuantity), quantity)
                .create();
    }

    /**
     * Creates a mock persisted Inventory with a specific Inventory ID.
     *
     * @param id The specific inventory ID (primary key) to set.
     * @return An Inventory object with the specified id.
     */
    public static Inventory mockPersistedObjectWithId(Integer id) {
        return Instancio.of(PERSISTED_INVENTORY_MODEL)
                .set(field(Inventory::getId), id)
                .create();
    }
}