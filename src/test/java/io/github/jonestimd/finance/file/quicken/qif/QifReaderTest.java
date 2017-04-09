package io.github.jonestimd.finance.file.quicken.qif;

import java.io.CharArrayReader;

import org.junit.Test;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;

public class QifReaderTest {
    @Test
    public void recordValuesMatchInput() throws Exception {
        String input = String.format("%sxxx\n%syyy\n%s\n", NAME.code(), DATE.code(), INCOME.code());
        QifReader reader = new QifReader(new CharArrayReader(input.toCharArray()));

        QifRecord record = reader.nextRecord();

        assertThat(record.getLines()).isEqualTo(3);
        assertThat(record.getValue(NAME)).isEqualTo("xxx");
        assertThat(record.getValue(DATE)).isEqualTo("yyy");
        assertThat(record.hasValue(INCOME)).isTrue();
    }

    @Test
    public void nextRecordReturnsAfterEndCode() throws Exception {
        String input = String.format("%sxxx\n^\n%syyy\n", NAME.code(), DATE.code());
        QifReader reader = new QifReader(new CharArrayReader(input.toCharArray()));

        QifRecord record = reader.nextRecord();

        assertThat(record.hasValue(NAME)).isTrue();
        assertThat(record.hasValue(DATE)).isFalse();
    }

    @Test
    public void nextRecordReturnsAfterControlCode() throws Exception {
        String input = String.format("%sxxx\n%szzz\n%syyy\n", NAME.code(), CONTROL.code(), DATE.code());
        QifReader reader = new QifReader(new CharArrayReader(input.toCharArray()));

        QifRecord record = reader.nextRecord();

        assertThat(record.hasValue(NAME)).isTrue();
        assertThat(record.hasValue(DATE)).isFalse();
    }
}
