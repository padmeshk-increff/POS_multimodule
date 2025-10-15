package com.increff.pos.config;

import com.increff.pos.utils.CamelToSnakeCase;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class TableNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        // Convert class name like "OrderItem" to snake_case like "order_item"
        String snakeCaseName = CamelToSnakeCase.convert(name.getText());

        // SPECIAL CASE: If this is the table for the Inventory class, keep it singular.
        if ("inventory".equalsIgnoreCase(snakeCaseName)) {
            return Identifier.toIdentifier("inventory");
        }

        // GENERAL RULE: For all other tables, add an "s" to pluralize.
        String pluralName = snakeCaseName + "s";

        return Identifier.toIdentifier(pluralName);
    }
}