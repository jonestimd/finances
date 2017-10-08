package io.github.jonestimd.finance.domain.fileimport;

import java.util.Collections;

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

    public ImportFieldBuilder dateFormat(String dateFormat) {
        field.setDateFormat(dateFormat);
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
        field.setRegion(new PageRegion(null, null, null, rightEdge, null, rightEdge));
        return this;
    }

    public ImportFieldBuilder bounds(Float top, Float bottom, Float labelLeft, Float labelRight, Float valueLeft, Float valueRight) {
        field.setRegion(new PageRegion(top, bottom, labelLeft, labelRight, valueLeft, valueRight));
        return this;
    }

    public ImportField get() {
        return field;
    }
}