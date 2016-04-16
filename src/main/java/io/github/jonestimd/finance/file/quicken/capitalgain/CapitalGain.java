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
import java.util.Date;
import java.util.function.Function;

import io.github.jonestimd.finance.file.quicken.QuickenException;

public interface CapitalGain {
    public static final Function<CapitalGain, BigDecimal> SALE_AMOUNT_ADAPTER = new Function<CapitalGain, BigDecimal>() {
        public BigDecimal apply(CapitalGain record) {
            try {
                return record.getNetSaleAmount();
            }
            catch (QuickenException ex) {
                throw new RuntimeException(ex);
            }
        }
    };
    public static final Function<CapitalGain, BigDecimal> SHARES_ADAPTER = new Function<CapitalGain, BigDecimal>() {
        public BigDecimal apply(CapitalGain record) {
            try {
                return record.getShares();
            }
            catch (QuickenException e) {
                throw new RuntimeException(e);
            }
        }
    };

    BigDecimal getShares() throws QuickenException;
    String getSecurityName() throws QuickenException;
    Date getPurchaseDate() throws QuickenException;
    Date getSellDate() throws QuickenException;
    BigDecimal getCostBasis() throws QuickenException;
    BigDecimal getNetSaleAmount() throws QuickenException;
}
