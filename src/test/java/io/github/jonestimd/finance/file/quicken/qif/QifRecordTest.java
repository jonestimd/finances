package io.github.jonestimd.finance.file.quicken.qif;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.QuickenRecord;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class QifRecordTest {
    @Test
    public void flagValuesForEmptyValues() {
        QifRecord record = new QifRecord(1L);

        assertThat(record.isOption()).isFalse();
        assertThat(record.isControl()).isFalse();
        assertThat(record.isEmpty()).isTrue();
        assertThat(record.isComplete()).isFalse();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void flagValuesForEndCode() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QuickenRecord.END, "");

        assertThat(record.isOption()).isFalse();
        assertThat(record.isControl()).isFalse();
        assertThat(record.isEmpty()).isTrue();
        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void flagValuesForAccountType() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.CONTROL, "Type:Bank");

        assertThat(record.isOption()).isFalse();
        assertThat(record.isControl()).isTrue();
        assertThat(record.isEmpty()).isFalse();
        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void flagValuesForClearAutoswitch() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.CONTROL, "Clear:Autoswitch");

        assertThat(record.isOption()).isTrue();
        assertThat(record.isControl()).isTrue();
        assertThat(record.isEmpty()).isFalse();
        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void flagValuesForCategory() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.NAME, "xxx");
        record.setValue(QuickenRecord.END, "");

        assertThat(record.isOption()).isFalse();
        assertThat(record.isControl()).isFalse();
        assertThat(record.isEmpty()).isFalse();
        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void flagValuesForMissingEndCode() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.NAME, "xxx");
        record.setValue(QifField.DESCRIPTION, "yyy");

        assertThat(record.isOption()).isFalse();
        assertThat(record.isControl()).isFalse();
        assertThat(record.isEmpty()).isFalse();
        assertThat(record.isComplete()).isFalse();
        assertThat(record.isValid()).isFalse();
    }

    @Test
    public void flagValuesForPartialCategory() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.NAME, "xxx");
        record.setValue(QifField.CONTROL, "Type:Bank");

        assertThat(record.isOption()).isFalse();
        assertThat(record.isControl()).isTrue();
        assertThat(record.isEmpty()).isFalse();
        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isFalse();
    }

    @Test
    public void multipleValuesForSplitTransaction() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.SPLIT_AMOUNT, "one");
        record.setValue(QifField.SPLIT_AMOUNT, "two");

        List<String> values = record.getValues(QifField.SPLIT_AMOUNT);

        assertThat(record.getLines()).isEqualTo(2);
        assertThat(values).hasSize(2);
        assertThat(values.get(0)).isEqualTo("one");
        assertThat(values.get(1)).isEqualTo("two");
    }

    @Test
    public void splitValuesCorrelate() throws Exception {
        String [] amounts = { "111", "222", "333" };
        String [] categories = { "one", "", "three" };
        String [] memos = { "", "bbb" };
        QifRecord record = new QifRecord(1L);
        for (int i=0; i<amounts.length; i++) {
            if (categories[i].length() > 0) record.setValue(QifField.SPLIT_CATEGORY, categories[i]);
            if (i < memos.length && memos[i].length() > 0) record.setValue(QifField.SPLIT_MEMO, memos[i]);
            record.setValue(QifField.SPLIT_AMOUNT, amounts[i]);
        }
        record.setValue(QuickenRecord.END, "");

        assertThat(record.getValues(QifField.SPLIT_AMOUNT).size() == record.getValues(QifField.SPLIT_CATEGORY).size()).isTrue();
        assertThat(record.getValues(QifField.SPLIT_AMOUNT).size() == record.getValues(QifField.SPLIT_MEMO).size()+1).isTrue();
        assertThat(record.getValues(QifField.SPLIT_CATEGORY)).isEqualTo(Arrays.asList(categories));
        assertThat(record.getValues(QifField.SPLIT_MEMO)).isEqualTo(Arrays.asList(memos));

        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void splitValuesWithMissingFields() throws Exception {
        String [] amounts = { "111", "222", "333" };
        String [] categories = { "one", "", "three" };
        String [] memos = { };
        QifRecord record = new QifRecord(1L);
        for (int i=0; i<amounts.length; i++) {
            if (categories[i].length() > 0) record.setValue(QifField.SPLIT_CATEGORY, categories[i]);
            if (i < memos.length && memos[i].length() > 0) record.setValue(QifField.SPLIT_MEMO, memos[i]);
            record.setValue(QifField.SPLIT_AMOUNT, amounts[i]);
        }
        record.setValue(QuickenRecord.END, "");

        assertThat(record.getValues(QifField.SPLIT_AMOUNT).size() == record.getValues(QifField.SPLIT_CATEGORY).size()).isTrue();
        assertThat(record.getValues(QifField.SPLIT_MEMO)).isEmpty();
        assertThat(record.getValues(QifField.SPLIT_CATEGORY)).isEqualTo(Arrays.asList(categories));
        assertThat(record.getValues(QifField.SPLIT_MEMO)).isEqualTo(Arrays.asList(memos));

        assertThat(record.isComplete()).isTrue();
        assertThat(record.isValid()).isTrue();
    }

    @Test
    public void getDate() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        testDateParsing("1/2/99", format.parse("01/02/1999"));
        testDateParsing("1/ 2/99", format.parse("01/02/1999"));
        testDateParsing("1/2'01", format.parse("01/02/2001"));
        testDateParsing("1/2' 1", format.parse("01/02/2001"));
        testDateParsing("1/2' 01", format.parse("01/02/2001"));
    }

    private void testDateParsing(String date, Date expected) throws QuickenException {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.DATE, date);
        assertThat(record.getDate(QifField.DATE)).isEqualTo(expected);
    }

    @Test
    public void getBigDecimal() throws Exception {
        testBigDecimalParsing("1000.00", 1000d);
        testBigDecimalParsing("-1000.00", -1000d);
        testBigDecimalParsing("1,000.00", 1000d);
        testBigDecimalParsing("-1,000.00", -1000d);
        testBigDecimalParsing("-1,000", -1000d);
        testBigDecimalParsing("-1000", -1000d);
        testBigDecimalParsing("-1,999,000", -1999000d);
        testBigDecimalParsing("-1,999,000.00", -1999000d);
        testBigDecimalParsing("1,999,000.00", 1999000d);
    }

    private void testBigDecimalParsing(String value, Double expected) {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.AMOUNT, value);
        assertThat(record.getBigDecimal(QifField.AMOUNT).doubleValue()).isCloseTo(expected, within(0d));
    }

    @Test
    public void getBigDecimalSplits() throws Exception {
        testBigDecimalSplitParsing("1000.00", 1000d);
        testBigDecimalSplitParsing("-1000.00", -1000d);
        testBigDecimalSplitParsing("1,000.00", 1000d);
        testBigDecimalSplitParsing("-1,000.00", -1000d);
        testBigDecimalSplitParsing("-1,000", -1000d);
        testBigDecimalSplitParsing("-1000", -1000d);
        testBigDecimalSplitParsing("-1,999,000", -1999000d);
        testBigDecimalSplitParsing("-1,999,000.00", -1999000d);
        testBigDecimalSplitParsing("1,999,000.00", 1999000d);
    }

    private void testBigDecimalSplitParsing(String value, Double expected) {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.SPLIT_AMOUNT, value);
        assertThat(record.getBigDecimals(QifField.SPLIT_AMOUNT).get(0).doubleValue()).isEqualTo(expected);
    }

    @Test
    public void isSplitForSingleSplitAmount() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.AMOUNT, "100.00");
        record.setValue(QifField.SPLIT_AMOUNT, "100.00");

        assertThat(record.isSplit()).isFalse();
    }

    @Test
    public void getBigDecimalDefaultsToZero() throws Exception {
        assertThat(new QifRecord(1L).getBigDecimal(QifField.AMOUNT).doubleValue()).isCloseTo(0d, within(0d));
    }

    @Test
    public void testGetCategoryAmount() throws Exception {
        String [] amounts = { "111", "222", "333", "555" };
        String [] categories = { "one", "two", "one", "one/xyz" };
        QifRecord record = new QifRecord(1L);
        for (int i=0; i<amounts.length; i++) {
            record.setValue(QifField.SPLIT_CATEGORY, categories[i]);
            record.setValue(QifField.SPLIT_AMOUNT, amounts[i]);
        }
        record.setValue(QuickenRecord.END, "");

        assertThat(record.getCategoryAmount(0).doubleValue()).isCloseTo(444d, within(0d));
        assertThat(record.getCategoryAmount(1).doubleValue()).isCloseTo(222d, within(0d));
        assertThat(record.getCategoryAmount(2).doubleValue()).isCloseTo(444d, within(0d));
        assertThat(record.getCategoryAmount(3).doubleValue()).isCloseTo(555d, within(0d));
    }
}