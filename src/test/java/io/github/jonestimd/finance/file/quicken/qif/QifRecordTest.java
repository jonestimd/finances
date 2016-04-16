package io.github.jonestimd.finance.file.quicken.qif;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.QuickenRecord;
import org.junit.Test;

import static junit.framework.Assert.*;

public class QifRecordTest {
    @Test
    public void flagValuesForEmptyValues() {
        QifRecord record = new QifRecord(1L);

        assertFalse(record.isOption());
        assertFalse(record.isControl());
        assertTrue(record.isEmpty());
        assertFalse(record.isComplete());
        assertTrue(record.isValid());
    }

    @Test
    public void flagValuesForEndCode() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QuickenRecord.END, "");

        assertFalse(record.isOption());
        assertFalse(record.isControl());
        assertTrue(record.isEmpty());
        assertTrue(record.isComplete());
        assertTrue(record.isValid());
    }

    @Test
    public void flagValuesForAccountType() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.CONTROL, "Type:Bank");

        assertFalse(record.isOption());
        assertTrue(record.isControl());
        assertFalse(record.isEmpty());
        assertTrue(record.isComplete());
        assertTrue(record.isValid());
    }

    @Test
    public void flagValuesForClearAutoswitch() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.CONTROL, "Clear:Autoswitch");

        assertTrue(record.isOption());
        assertTrue(record.isControl());
        assertFalse(record.isEmpty());
        assertTrue(record.isComplete());
        assertTrue(record.isValid());
    }

    @Test
    public void flagValuesForCategory() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.NAME, "xxx");
        record.setValue(QuickenRecord.END, "");

        assertFalse(record.isOption());
        assertFalse(record.isControl());
        assertFalse(record.isEmpty());
        assertTrue(record.isComplete());
        assertTrue(record.isValid());
    }

    @Test
    public void flagValuesForMissingEndCode() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.NAME, "xxx");
        record.setValue(QifField.DESCRIPTION, "yyy");

        assertFalse(record.isOption());
        assertFalse(record.isControl());
        assertFalse(record.isEmpty());
        assertFalse(record.isComplete());
        assertFalse(record.isValid());
    }

    @Test
    public void flagValuesForPartialCategory() {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.NAME, "xxx");
        record.setValue(QifField.CONTROL, "Type:Bank");

        assertFalse(record.isOption());
        assertTrue(record.isControl());
        assertFalse(record.isEmpty());
        assertTrue(record.isComplete());
        assertFalse(record.isValid());
    }

    @Test
    public void multipleValuesForSplitTransaction() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.SPLIT_AMOUNT, "one");
        record.setValue(QifField.SPLIT_AMOUNT, "two");

        List<String> values = record.getValues(QifField.SPLIT_AMOUNT);

        assertEquals(2, record.getLines());
        assertEquals(2, values.size());
        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
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

        assertTrue(record.getValues(QifField.SPLIT_AMOUNT).size() == record.getValues(QifField.SPLIT_CATEGORY).size());
        assertTrue(record.getValues(QifField.SPLIT_AMOUNT).size() == record.getValues(QifField.SPLIT_MEMO).size()+1);
        assertEquals(Arrays.asList(categories), record.getValues(QifField.SPLIT_CATEGORY));
        assertEquals(Arrays.asList(memos), record.getValues(QifField.SPLIT_MEMO));

        assertTrue(record.isComplete());
        assertTrue(record.isValid());
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

        assertTrue(record.getValues(QifField.SPLIT_AMOUNT).size() == record.getValues(QifField.SPLIT_CATEGORY).size());
        assertEquals(0, record.getValues(QifField.SPLIT_MEMO).size());
        assertEquals(Arrays.asList(categories), record.getValues(QifField.SPLIT_CATEGORY));
        assertEquals(Arrays.asList(memos), record.getValues(QifField.SPLIT_MEMO));

        assertTrue(record.isComplete());
        assertTrue(record.isValid());
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
        assertEquals(expected, record.getDate(QifField.DATE));
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
        assertEquals(expected, record.getBigDecimal(QifField.AMOUNT).doubleValue(), 0d);
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
        assertEquals(expected, record.getBigDecimals(QifField.SPLIT_AMOUNT).get(0).doubleValue());
    }

    @Test
    public void isSplitForSingleSplitAmount() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.AMOUNT, "100.00");
        record.setValue(QifField.SPLIT_AMOUNT, "100.00");

        assertFalse(record.isSplit());
    }

    @Test
    public void getBigDecimalDefaultsToZero() throws Exception {
        assertEquals(0d, new QifRecord(1L).getBigDecimal(QifField.AMOUNT).doubleValue(), 0d);
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

        assertEquals(444d, record.getCategoryAmount(0).doubleValue(), 0d);
        assertEquals(222d, record.getCategoryAmount(1).doubleValue(), 0d);
        assertEquals(444d, record.getCategoryAmount(2).doubleValue(), 0d);
        assertEquals(555d, record.getCategoryAmount(3).doubleValue(), 0d);
    }
}