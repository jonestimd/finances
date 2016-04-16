package io.github.jonestimd.finance.file.quicken.qif;

import java.io.CharArrayReader;

import org.junit.Test;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static junit.framework.Assert.*;

public class QifReaderTest {
    @Test
    public void recordValuesMatchInput() throws Exception {
        String input = String.format("%sxxx\n%syyy\n%s\n", NAME.code(), DATE.code(), INCOME.code());
        QifReader reader = new QifReader(new CharArrayReader(input.toCharArray()));

        QifRecord record = reader.nextRecord();

        assertEquals(3, record.getLines());
        assertEquals("xxx", record.getValue(NAME));
        assertEquals("yyy", record.getValue(DATE));
        assertTrue(record.hasValue(INCOME));
    }

    @Test
    public void nextRecordReturnsAfterEndCode() throws Exception {
        String input = String.format("%sxxx\n^\n%syyy\n", NAME.code(), DATE.code());
        QifReader reader = new QifReader(new CharArrayReader(input.toCharArray()));

        QifRecord record = reader.nextRecord();

        assertTrue(record.hasValue(NAME));
        assertFalse(record.hasValue(DATE));
    }

    @Test
    public void nextRecordReturnsAfterControlCode() throws Exception {
        String input = String.format("%sxxx\n%szzz\n%syyy\n", NAME.code(), CONTROL.code(), DATE.code());
        QifReader reader = new QifReader(new CharArrayReader(input.toCharArray()));

        QifRecord record = reader.nextRecord();

        assertTrue(record.hasValue(NAME));
        assertFalse(record.hasValue(DATE));
    }
}
