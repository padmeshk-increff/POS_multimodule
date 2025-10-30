package com.increff.pos.factory; // Or any test package you prefer

import com.increff.pos.entity.Client;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating Client entities using Instancio.
 * This class provides standardized objects for use in unit tests.
 *
 * It includes overloaded methods to set common, non-randomized
 * fields like 'id' and 'clientName'.
 */
public final class ClientFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ClientFactory() {
    }

    /**
     * Model for a 'new' Client, as if it came from a form.
     * It will always have a null ID.
     */
    private static final Model<Client> NEW_CLIENT_MODEL = Instancio.of(Client.class)
            .set(field(Client::getId), null)
            .toModel();

    /**
     * Model for a 'persisted' Client, as if it were loaded from the DB.
     * It will always have a non-null, positive ID.
     */
    private static final Model<Client> PERSISTED_CLIENT_MODEL = Instancio.of(Client.class)
            .generate(field(Client::getId), gen -> gen.ints().min(1))
            .toModel();


    // --- Methods for NEW objects (ID is always null) ---

    /**
     * Creates a mock Client object representing a new entity that has not been saved.
     * Its ID will always be null and all other fields are randomized.
     *
     * @return A new Client object with a null ID.
     */
    public static Client mockNewObject() {
        // Use of(MODEL) instead of withSettings(MODEL)
        return Instancio.of(NEW_CLIENT_MODEL)
                .create();
    }

    /**
     * Creates a mock Client object representing a new entity with a specific name.
     * Its ID will always be null.
     *
     * @param clientName The specific name to set on the object.
     * @return A new Client object with a null ID and the specified name.
     */
    public static Client mockNewObject(String clientName) {
        // Use of(MODEL) instead of withSettings(MODEL)
        return Instancio.of(NEW_CLIENT_MODEL)
                .set(field(Client::getClientName), clientName)
                .create();
    }

    // --- Methods for PERSISTED objects (ID is non-null) ---

    /**
     * Creates a mock Client object representing a persisted entity from the database.
     * Its ID will be a non-null, positive random integer, and all other fields are randomized.
     *
     * @return A Client object with a non-null random ID.
     */
    public static Client mockPersistedObject() {
        // Use of(MODEL) instead of withSettings(MODEL)
        return Instancio.of(PERSISTED_CLIENT_MODEL)
                .create();
    }

    /**
     * Creates a mock Client object representing a persisted entity with a specific ID.
     * All other fields are randomized.
     *
     * @param id The specific ID to set on the object.
     * @return A Client object with the specified ID.
     */
    public static Client mockPersistedObject(Integer id) {
        // Use of(MODEL) instead of withSettings(MODEL)
        return Instancio.of(PERSISTED_CLIENT_MODEL)
                .set(field(Client::getId), id)
                .create();
    }

    /**
     * Creates a mock Client object representing a persisted entity with a specific name.
     * Its ID will be a non-null, positive random integer.
     *
     * @param clientName The specific name to set on the object.
     * @return A Client object with a random ID and the specified name.
     */
    public static Client mockPersistedObject(String clientName) {
        // Use of(MODEL) instead of withSettings(MODEL)
        return Instancio.of(PERSISTED_CLIENT_MODEL)
                .set(field(Client::getClientName), clientName)
                .create();
    }

    /**
     * Creates a mock Client object representing a persisted entity with a specific ID and name.
     *
     * @param id The specific ID to set on the object.
     * @param clientName The specific name to set on the object.
     * @return A Client object with the specified ID and name.
     */
    public static Client mockPersistedObject(Integer id, String clientName) {
        // Use of(MODEL) instead of withSettings(MODEL)
        return Instancio.of(PERSISTED_CLIENT_MODEL)
                .set(field(Client::getId), id)
                .set(field(Client::getClientName), clientName)
                .create();
    }
}