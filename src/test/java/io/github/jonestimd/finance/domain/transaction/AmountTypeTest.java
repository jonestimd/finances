package io.github.jonestimd.finance.domain.transaction;

import io.github.jonestimd.finance.swing.BundleType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AmountTypeTest {
    @Test
    public void toStringReturnsLocalizedValues() {
        for (AmountType type : AmountType.values()) {
            assertThat(type.toString()).isEqualTo(BundleType.REFERENCE.getString("amountType." + type.name()));
        }
    }
}