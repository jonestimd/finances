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
package io.github.jonestimd.finance.domain.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.persistence.Embeddable;

@Embeddable
public class SplitRatio {
    private BigDecimal sharesIn = BigDecimal.ONE;
    private BigDecimal sharesOut = BigDecimal.ONE;

    public SplitRatio() {}

    public SplitRatio(BigDecimal sharesIn, BigDecimal sharesOut) {
        this.sharesIn = sharesIn;
        this.sharesOut = sharesOut;
    }

    public BigDecimal getSharesIn() {
        return sharesIn;
    }

    public void setSharesIn(BigDecimal sharesIn) {
        this.sharesIn = sharesIn;
    }

    public BigDecimal getSharesOut() {
        return sharesOut;
    }

    public void setSharesOut(BigDecimal sharesOut) {
        this.sharesOut = sharesOut;
    }

    public BigDecimal getRatio(int scale) {
        return sharesOut.divide(sharesIn, scale, RoundingMode.HALF_EVEN);
    }

    public SplitRatio multiply(SplitRatio ratio) {
        return new SplitRatio(sharesIn.multiply(ratio.sharesIn), sharesOut.multiply(ratio.sharesOut));
    }

    public BigDecimal apply(BigDecimal shares, int scale) {
        return shares.multiply(sharesOut).divide(sharesIn, scale, RoundingMode.HALF_EVEN);
    }

    public BigDecimal revert(BigDecimal shares, int scale) {
        return shares.multiply(sharesIn).divide(sharesOut, scale, RoundingMode.HALF_EVEN);
    }
}