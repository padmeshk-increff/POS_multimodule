package com.increff.pos.factory;

import com.increff.pos.entity.Product;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating Product entities using Instancio.
 */
public final class ProductFactory {

    private ProductFactory() {
    }

    /**
     * Model for a 'new' Product, not yet saved.
     * ID is null.
     */
    private static final Model<Product> NEW_PRODUCT_MODEL = Instancio.of(Product.class)
            .set(field(Product::getId), null) // A new object has no ID
            // THE FIX: Use .upperCase() before .length()
            .generate(field(Product::getBarcode), gen -> gen.string()
                    .alphaNumeric()
                    .upperCase()
                    .length(10))
            .generate(field(Product::getName), gen -> gen.string().length(8))
            .generate(field(Product::getCategory), gen -> gen.string().length(6))
            .generate(field(Product::getClientId), gen -> gen.ints().min(1))
            .generate(field(Product::getMrp), gen -> gen.doubles().min(1.0).max(5000.0))
            .toModel();

    /**
     * Model for a 'persisted' Product, as if from the DB.
     * ID is a positive integer.
     */
    private static final Model<Product> PERSISTED_PRODUCT_MODEL = Instancio.of(Product.class)
            .generate(field(Product::getId), gen -> gen.ints().min(1))
            .generate(field(Product::getBarcode), gen -> gen.string()
                    .alphaNumeric()
                    .upperCase()
                    .length(10))
            .generate(field(Product::getName), gen -> gen.string().length(8))
            .generate(field(Product::getCategory), gen -> gen.string().length(6))
            .generate(field(Product::getClientId), gen -> gen.ints().min(1))
            .generate(field(Product::getMrp), gen -> gen.doubles().min(1.0).max(5000.0))
            .toModel();

    /**
     * Creates a mock 'new' Product object (ID is null).
     * @param barcode The specific barcode to set.
     * @param clientId The specific client ID to set.
     * @return A new Product object.
     */
    public static Product mockNewObject(String barcode, Integer clientId) {
        return Instancio.of(NEW_PRODUCT_MODEL)
                .set(field(Product::getBarcode), barcode)
                .set(field(Product::getClientId), clientId)
                .create();
    }

    public static Product mockNewObject(Integer clientId){
        return Instancio.of(NEW_PRODUCT_MODEL)
                .set(field(Product::getClientId), clientId)
                .create();
    }

    public static Product mockNewObject(Integer clientId,Double mrp){
        return Instancio.of(NEW_PRODUCT_MODEL)
                .set(field(Product::getClientId), clientId)
                .set(field(Product::getMrp), mrp)
                .create();
    }
    /**
     * Creates a mock 'persisted' Product object.
     * @param id The specific ID to set.
     * @return A persisted Product object.
     */
    public static Product mockPersistedObject(Integer id) {
        return Instancio.of(PERSISTED_PRODUCT_MODEL)
                .set(field(Product::getId), id)
                .create();
    }

    /**
     * Creates a mock 'persisted' Product object with a specific barcode.
     * @param barcode The specific barcode to set.
     * @return A persisted Product object.
     */
    public static Product mockPersistedObject(String barcode) {
        return Instancio.of(PERSISTED_PRODUCT_MODEL)
                .set(field(Product::getBarcode), barcode)
                .create();
    }

    /**
     * Creates a mock 'persisted' Product object with specific details for update tests.
     * @param id The specific ID (PK).
     * @param barcode The specific barcode.
     * @param clientId The specific client ID (FK).
     * @return A persisted Product object.
     */
    public static Product mockPersistedObject(Integer id, String barcode, Integer clientId) {
        return Instancio.of(PERSISTED_PRODUCT_MODEL)
                .set(field(Product::getId), id)
                .set(field(Product::getBarcode), barcode)
                .set(field(Product::getClientId), clientId)
                .create();
    }
}