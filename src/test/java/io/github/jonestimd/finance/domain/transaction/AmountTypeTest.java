package io.github.jonestimd.finance.domain.transaction;

import io.github.jonestimd.finance.swing.BundleType;
import org.junit.Test;

import static org.junit.Assert.*;

public class AmountTypeTest {
    @Test
    public void toStringReturnsLocalizedValues() {
        for (AmountType type : AmountType.values()) {
            assertEquals(BundleType.REFERENCE.getString("amountType." + type.name()), type.toString());
        }
    }
}