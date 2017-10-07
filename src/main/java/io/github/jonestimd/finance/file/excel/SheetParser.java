// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.file.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.github.jonestimd.finance.file.FileParser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;

public class SheetParser implements FileParser {
    private final List<Map<String, String>> rows = new ArrayList<>();

    public SheetParser(InputStream stream, int sheetIndex, int headerRow) throws IOException {
        this(new HSSFWorkbook(new NPOIFSFileSystem(stream).getRoot(), true).getSheetAt(sheetIndex), headerRow);
    }

    public SheetParser(Sheet sheet, int headerRow) {
        final DataFormatter formatter = new DataFormatter();
        final Map<Integer, String> columnNames = new HashMap<>();
        final int lastRow = sheet.getLastRowNum();
        sheet.getRow(headerRow).forEach(cell -> columnNames.put(cell.getColumnIndex(), formatter.formatCellValue(cell)));
        for (int index = headerRow + 1; index < lastRow; index++) {
            rows.add(getRow(sheet, index, columnNames, formatter));
        }
    }

    private Map<String, String> getRow(Sheet sheet, int index, Map<Integer, String> columnNames, DataFormatter formatter) {
        Map<String, String> values = new HashMap<>();
        for (Cell cell : sheet.getRow(index++)) {
            String key = columnNames.get(cell.getColumnIndex());
            if (key != null) {
                if (cell.getCellTypeEnum() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) values.put(key, String.valueOf(cell.getNumericCellValue()));
                else values.put(key, formatter.formatCellValue(cell));
            }
        }
        return values;
    }

    @Override
    public Stream<Map<String, String>> getStream() {
        return rows.stream();
    }
}
