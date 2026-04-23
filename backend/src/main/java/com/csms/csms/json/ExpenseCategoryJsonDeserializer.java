package com.csms.csms.json;

import com.csms.csms.entity.ExpenseCategory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class ExpenseCategoryJsonDeserializer extends JsonDeserializer<ExpenseCategory> {

    @Override
    public ExpenseCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            return ExpenseCategory.valueOf(s);
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(ExpenseCategory.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw InvalidFormatException.from(p,
                    "Unknown expense category '" + raw + "'. Use one of: " + allowed
                            + ". If ELECTRICITY/WATER/GAS fail at save time, run db/add_expense_utility_categories.sql on Postgres.",
                    raw,
                    ExpenseCategory.class);
        }
    }
}
