package io.github.jonestimd.finance.swing.asset;

import java.util.Arrays;
import java.util.Date;

import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.asset.SplitDateValidator.*;
import static org.assertj.core.api.Assertions.*;

public class SplitDateValidatorTest {
    private SplitDateValidator validator = new SplitDateValidator();

    @Test
    public void validateReturnsRequiredMessageForNull() throws Exception {
        assertThat(validator.validate(-1, null, null)).isEqualTo(LABELS.getString(REQUIRED_MESSAGE));
    }

    @Test
    public void validateReturnsUniqueMessageForDuplicateDate() throws Exception {
        Date date = new Date();

        assertThat(validator.validate(0, date, Arrays.asList(newSplit(date), newSplit(date)))).isEqualTo(LABELS.getString(UNIQUE_MESSAGE));
    }

    @Test
    public void validateReturnsNullForUniqueDate() throws Exception {
        Date date1 = new Date();
        Date date2 = DateUtils.addDays(new Date(), 1);

        assertThat(validator.validate(0, date1, Arrays.asList(newSplit(date1), newSplit(date2)))).isNullOrEmpty();
    }

    private StockSplit newSplit(Date date) {
        return new StockSplit(null, date, new SplitRatio());
    }
}