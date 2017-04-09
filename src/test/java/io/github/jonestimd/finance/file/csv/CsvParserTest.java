package io.github.jonestimd.finance.file.csv;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CsvParserTest {
    @Test
    public void handlesEmptyFile() throws Exception {
        CsvParser parser = new CsvParser(new ByteArrayInputStream("".getBytes()));

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).isEmpty();
    }

    @Test
    public void handlesNoDataRows() throws Exception {
        CsvParser parser = new CsvParser(getInputStream());

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).isEmpty();
    }

    @Test
    public void ignoresEmptyLines() throws Exception {
        CsvParser parser = new CsvParser(getInputStream("\n"));

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).isEmpty();
    }

    @Test
    public void parsesRows() throws Exception {
        CsvParser parser = new CsvParser(getInputStream("value1,value2,value3"));

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).hasSize(1);
        checkHeader(rows);
        assertThat(rows.get(0).get("Header 1")).isEqualTo("value1");
        assertThat(rows.get(0).get("Header 2")).isEqualTo("value2");
        assertThat(rows.get(0).get("Header 3")).isEqualTo("value3");
    }

    protected void checkHeader(List<Map<String, String>> rows) {
        assertThat(rows.get(0).keySet()).containsOnly("Header 1", "Header 2", "Header 3");
    }

    @Test
    public void removesQuotes() throws Exception {
        CsvParser parser = new CsvParser(getInputStream("value1,\"value2\",\"value, 3\"\r\n\"\",abc,\"xyz,123\""));

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).hasSize(2);
        checkHeader(rows);
        assertThat(rows.get(0).get("Header 1")).isEqualTo("value1");
        assertThat(rows.get(0).get("Header 2")).isEqualTo("value2");
        assertThat(rows.get(0).get("Header 3")).isEqualTo("value, 3");
        assertThat(rows.get(1).get("Header 1")).isEqualTo("");
        assertThat(rows.get(1).get("Header 2")).isEqualTo("abc");
        assertThat(rows.get(1).get("Header 3")).isEqualTo("xyz,123");
    }

    @Test
    public void handlesEmptyValues() throws Exception {
        CsvParser parser = new CsvParser(getInputStream(",value2\r\nvalue A,,value B"));

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).hasSize(2);
        checkHeader(rows);
        assertThat(rows.get(0).get("Header 1")).isEqualTo("");
        assertThat(rows.get(0).get("Header 2")).isEqualTo("value2");
        assertThat(rows.get(0).get("Header 3")).isEqualTo("");
        assertThat(rows.get(1).get("Header 1")).isEqualTo("value A");
        assertThat(rows.get(1).get("Header 2")).isEqualTo("");
        assertThat(rows.get(1).get("Header 3")).isEqualTo("value B");
    }

    protected InputStream getInputStream(String... lines) {
        final String content = "Header 1,Header 2,Header 3\r\n" + Joiner.on("\r\n").join(lines);
        return new ByteArrayInputStream(content.getBytes());
    }
}