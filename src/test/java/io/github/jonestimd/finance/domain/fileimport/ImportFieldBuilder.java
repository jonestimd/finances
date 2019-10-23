package io.github.jonestimd.finance.domain.fileimport;

import java.util.Collections;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;

public class ImportFieldBuilder {
    private final ImportField field = new ImportField();

    public ImportFieldBuilder type(FieldType type) {
        field.setType(type);
        return this;
    }

    public ImportFieldBuilder label(String label) {
        field.setLabels(Collections.singletonList(label));
        return this;
    }

    public ImportFieldBuilder category(TransactionCategory category) {
        field.setCategory(category);
        return this;
    }

    public ImportFieldBuilder account(Account account) {
        field.setTransferAccount(account);
        return this;
    }

    public ImportFieldBuilder amountFormat(AmountFormat amountFormat) {
        field.setAmountFormat(amountFormat);
        return this;
    }

    public ImportFieldBuilder negate(boolean negate) {
        field.setNegate(negate);
        return this;
    }

    public ImportFieldBuilder rightEdge(Float rightEdge) {
        field.setRegion(new PageRegion("region", null, null, null, rightEdge, null, rightEdge));
        return this;
    }

    public ImportFieldBuilder bounds(Float top, Float bottom, Float labelLeft, Float labelRight, Float valueLeft, Float valueRight) {
        field.setRegion(new PageRegion("region", top, bottom, labelLeft, labelRight, valueLeft, valueRight));
        return this;
    }

    public ImportField get() {
        return field;
    }
}