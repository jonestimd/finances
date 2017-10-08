package io.github.jonestimd.finance.file.excel;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Streams;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SheetParserTest {
    @Mock
    private Sheet sheet;

    @Test
    public void usesFirstRowForFieldNames() throws Exception {
        Row headerRow = mock(Row.class);
        Row dataRow = mock(Row.class);
        when(sheet.getLastRowNum()).thenReturn(2);
        when(sheet.getRow(1)).thenReturn(headerRow);
        when(sheet.getRow(2)).thenReturn(dataRow);
        doAnswer(new RowForEachAnswer("column1", "column2")).when(headerRow).forEach(any());
        mockIterator(dataRow, "value1", "value2", "value3");
        SheetParser parser = new SheetParser(sheet, 1);

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).hasSize(2);
        assertThat(rows.get(0).get("column1")).isEqualTo("value1");
        assertThat(rows.get(0).get("column2")).isEqualTo("value2");
    }

    @SuppressWarnings("unchecked")
    private void mockIterator(Row row, String... values) {
        List<Cell> cells = Streams.mapWithIndex(Arrays.stream(values), SheetParserTest::mockCell).collect(Collectors.toList());
        when(row.iterator()).thenReturn(cells.iterator());
    }

    @Test
    public void convertsNumberToString() throws Exception {
        Row headerRow = mock(Row.class);
        Row dataRow = mock(Row.class);
        when(sheet.getLastRowNum()).thenReturn(2);
        when(sheet.getRow(1)).thenReturn(headerRow);
        when(sheet.getRow(2)).thenReturn(dataRow);
        doAnswer(new RowForEachAnswer("column1")).when(headerRow).forEach(any());
        mockIterator(dataRow, mockCell(123.456, 0));
        SheetParser parser = new SheetParser(sheet, 1);

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).hasSize(1);
        assertThat(rows.get(0).get("column1")).isEqualTo("123.456");
    }

    @Test
    public void convertsDateToString() throws Exception {
        Row headerRow = mock(Row.class);
        Row dataRow = mock(Row.class);
        when(sheet.getLastRowNum()).thenReturn(2);
        when(sheet.getRow(1)).thenReturn(headerRow);
        when(sheet.getRow(2)).thenReturn(dataRow);
        doAnswer(new RowForEachAnswer("column1")).when(headerRow).forEach(any());
        Date value = new Date();
        mockIterator(dataRow, mockCell(value, 0));
        SheetParser parser = new SheetParser(sheet, 1);

        List<Map<String, String>> rows = parser.getStream().collect(Collectors.toList());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).hasSize(1);
        assertThat(rows.get(0).get("column1")).isEqualTo(value.toString());
    }

    @SuppressWarnings("unchecked")
    private void mockIterator(Row row, Cell... cells) {
        when(row.iterator()).thenReturn(Arrays.asList(cells).iterator());
    }

    private static Cell mockCell(String text, long index) {
        Cell cell = mock(Cell.class);
        RichTextString value = mock(RichTextString.class);
        when(value.getString()).thenReturn(text);
        when(cell.getColumnIndex()).thenReturn((int) index);
        when(cell.getCellTypeEnum()).thenReturn(CellType.STRING);
        when(cell.getRichStringCellValue()).thenReturn(value);
        return cell;
    }

    private static Cell mockCell(double value, long index) {
        Cell cell = mock(Cell.class);
        when(cell.getColumnIndex()).thenReturn((int) index);
        when(cell.getCellTypeEnum()).thenReturn(CellType.NUMERIC);
        when(cell.getNumericCellValue()).thenReturn(value);
        return cell;
    }

    private static Cell mockCell(Date value, long index) {
        Cell cell = mock(Cell.class);
        when(cell.getColumnIndex()).thenReturn((int) index);
        when(cell.getCellTypeEnum()).thenReturn(CellType.NUMERIC);
        CellStyle style = mock(CellStyle.class);
        when(style.getDataFormat()).thenReturn((short) 0x0e);
        when(style.getDataFormatString()).thenReturn("");
        when(cell.getCellStyle()).thenReturn(style);
        when(cell.getDateCellValue()).thenReturn(value);
        return cell;
    }

    private static class RowForEachAnswer implements Answer<Cell> {
        private final List<String> values;

        private RowForEachAnswer(String... values) {
            this.values = Arrays.asList(values);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Cell answer(InvocationOnMock invocation) throws Throwable {
            Consumer<Cell> consumer = (Consumer<Cell>) invocation.getArguments()[0];
            IntStream.range(0, values.size()).mapToObj(this::mockCell).forEach(consumer);
            return null;
        }

        private Cell mockCell(int index) {
            return SheetParserTest.mockCell(values.get(index), index);
        }
    }
}