package com.increff.pos.factory;

import com.increff.pos.entity.User;
import com.increff.pos.model.enums.Role;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating User entities using Instancio.
 */
public final class UserFactory {

    private UserFactory() {
    }

    /**
     * Model for a 'new' User, not yet saved.
     * ID is null. Role is null (will be set by flow).
     */
    private static final Model<User> NEW_USER_MODEL = Instancio.of(User.class)
            .set(field(User::getId), null) // A new object has no ID
            .set(field(User::getRole), null) // Role is not set yet
            .generate(field(User::getEmail), gen -> gen.net().email())
            .toModel();

    /**
     * Model for a 'persisted' User, as if from the DB.
     * ID is a positive integer. Email and Role are randomized.
     */
    private static final Model<User> PERSISTED_USER_MODEL = Instancio.of(User.class)
            .generate(field(User::getId), gen -> gen.ints().min(1)) // Random positive ID
            .generate(field(User::getEmail), gen -> gen.net().email()) // Random plausible email
            // Instancio will automatically pick a random Role enum value.
            .toModel();

    /**
     * Creates a mock 'new' User object representing form input.
     * ID and Role are null, Password is raw.
     * @param email The specific email.
     * @param rawPassword The raw, un-encoded password.
     * @return A new User object.
     */
    public static User mockNewObject(String email, String rawPassword) {
        return Instancio.of(NEW_USER_MODEL)
                .set(field(User::getEmail), email)
                .set(field(User::getPassword), rawPassword)
                .create();
    }

    public static User mockNewObject(String email, String rawPassword, Role role) {
        return Instancio.of(NEW_USER_MODEL)
                .set(field(User::getEmail), email)
                .set(field(User::getPassword), rawPassword)
                .set(field(User::getRole), role)
                .create();
    }

    /**
     * Creates a mock 'persisted' User object.
     * @param email The specific email.
     * @param encodedPassword The specific (hashed) password.
     * @param role The user's role.
     * @return A persisted User object.
     */
    public static User mockPersistedObject(String email, String encodedPassword, Role role) {
        return Instancio.of(PERSISTED_USER_MODEL)
                .set(field(User::getEmail), email)
                .set(field(User::getPassword), encodedPassword)
                .set(field(User::getRole), role)
                .create();
    }
}