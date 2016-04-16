// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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
package io.github.jonestimd.finance.file.quicken.capitalgain;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.swing.BundleType;

public class CapitalGainRecord implements CapitalGain {
    private final long lineNumber;
    private final Map<String, String> values;

    public CapitalGainRecord(long lineNumber, Map<String, String> values) {
        this.lineNumber = lineNumber;
        this.values = values;
    }

    @Override
    public BigDecimal getShares() throws QuickenException {
        assertValueExists(CapitalGainColumn.Shares);
        return parseNumber(CapitalGainColumn.Shares);
    }

    @Override
    public String getSecurityName() throws QuickenException {
        assertValueExists(CapitalGainColumn.Security);
        return values.get(CapitalGainColumn.Security.getHeader());
    }

    @Override
    public Date getPurchaseDate() throws QuickenException {
        return parseDate(CapitalGainColumn.Bought);
    }

    @Override
    public Date getSellDate() throws QuickenException {
        return parseDate(CapitalGainColumn.Sold);
    }

    @Override
    public BigDecimal getCostBasis() throws QuickenException {
        return parseNumber(CapitalGainColumn.CostBasis);
    }

    @Override
    public BigDecimal getNetSaleAmount() throws QuickenException {
        return parseNumber(CapitalGainColumn.SalesPrice);
    }

    private BigDecimal parseNumber(CapitalGainColumn column) throws QuickenException {
        try {
            return new BigDecimal(values.get(column.getHeader()).replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new QuickenException("invalidRecord", "TSV", lineNumber);
        }
    }

    private Date parseDate(CapitalGainColumn column) throws QuickenException {
        assertValueExists(column);
        try {
            return new SimpleDateFormat(BundleType.QUICKEN_CAPITAL_GAIN.getString("DateFormat")).parse(values.get(column.getHeader()));
        }
        catch (ParseException ex) {
            throw new QuickenException("invalidDate", values.get(column.getHeader()), lineNumber);
        }
    }

    private void assertValueExists(CapitalGainColumn column) throws QuickenException {
        if (! values.containsKey(column.getHeader())) {
            throw new QuickenException("invalidRecord", "TSV", lineNumber);
        }
    }

    public String toString() {
        return values.toString();
    }
}
