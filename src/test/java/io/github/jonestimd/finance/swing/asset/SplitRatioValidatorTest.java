package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import org.junit.Test;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

public class SplitRatioValidatorTest {
    private SplitRatioValidator sharesInValidator = new SplitRatioValidator("sharesIn");
    private SplitRatioValidator sharesOutValidator = new SplitRatioValidator("sharesOut");

    @Test
    public void validateReturnsRequiredMessageForNull() throws Exception {
        assertThat(sharesInValidator.validate(-1, null, null)).isEqualTo(sharesInValidator.requiredMessage);
        assertThat(sharesOutValidator.validate(-1, null, null)).isEqualTo(sharesOutValidator.requiredMessage);
    }

    @Test
    public void validateReturnsInvalidMessageForRatioOfOne() throws Exception {
        StockSplit split = new StockSplit(null, null, new SplitRatio());

        assertThat(sharesInValidator.validate(0, BigDecimal.ONE, singletonList(split))).isEqualTo(sharesInValidator.invalidSplitMessage);
        assertThat(sharesOutValidator.validate(0, BigDecimal.ONE, singletonList(split))).isEqualTo(sharesInValidator.invalidSplitMessage);
    }

    @Test
    public void validateReturnsNullForValidRatio() throws Exception {
        StockSplit split = new StockSplit(null, null, new SplitRatio(BigDecimal.ONE, BigDecimal.TEN));

        assertThat(sharesInValidator.validate(0, BigDecimal.ONE, singletonList(split))).isNull();
        assertThat(sharesOutValidator.validate(0, BigDecimal.TEN, singletonList(split))).isNull();
    }
}