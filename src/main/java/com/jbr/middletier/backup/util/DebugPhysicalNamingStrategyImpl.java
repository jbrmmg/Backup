package com.jbr.middletier.backup.util;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.io.Serializable;

@SuppressWarnings("unused")
/*
 * This class is only used when running in debug - which uses H2 database and some column names need to be modified.
 */
public class DebugPhysicalNamingStrategyImpl implements PhysicalNamingStrategy, Serializable {
    public static final DebugPhysicalNamingStrategyImpl INSTANCE = new DebugPhysicalNamingStrategyImpl();

    @Override
    public Identifier toPhysicalCatalogName(final Identifier name, final JdbcEnvironment context) {
        if(name == null) {
            return null;
        }

        return new Identifier(name.getText(), false);
    }

    @Override
    public Identifier toPhysicalSchemaName(final Identifier name, final JdbcEnvironment context) {
        if(name == null) {
            return null;
        }

        return new Identifier(name.getText(), false);
    }

    @Override
    public Identifier toPhysicalTableName(final Identifier name, final JdbcEnvironment context) {

        return new Identifier(name.getText(), false);
    }

    @Override
    public Identifier toPhysicalSequenceName(final Identifier name, final JdbcEnvironment context) {

        return new Identifier(name.getText(), false);
    }

    @Override
    public Identifier toPhysicalColumnName(final Identifier name, final JdbcEnvironment context) {
        // For H2 add quotes.
        if(name.getText().equalsIgnoreCase("filter")) {
            return new Identifier(name.getText(), true);
        }

        // For H2 need to change
        if(name.getText().equalsIgnoreCase("classificationid")) {
            return new Identifier("classification_id",false);
        }

        return new Identifier(name.getText(), false);
    }
}
