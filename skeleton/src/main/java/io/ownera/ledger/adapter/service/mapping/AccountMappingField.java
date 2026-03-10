package io.ownera.ledger.adapter.service.mapping;

public class AccountMappingField {
    private final String field;
    private final String description;
    private final String exampleValue;

    public AccountMappingField(String field, String description, String exampleValue) {
        this.field = field;
        this.description = description;
        this.exampleValue = exampleValue;
    }

    public String getField() { return field; }
    public String getDescription() { return description; }
    public String getExampleValue() { return exampleValue; }
}
