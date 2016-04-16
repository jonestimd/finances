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
package io.github.jonestimd.finance.file.quicken.qif;

import io.github.jonestimd.finance.file.quicken.QuickenRecord;

public enum QifField {
    CONTROL('!'),
    NAME('N'),
    TYPE('T'),
    DESCRIPTION('D'),
    INCOME('I'),
    EXPENSE('E'),
    TAX_RELATED('T'),
    TAX_SCHEDULE('R'),
    BUDGET_AMOUNT('B'),
    NUMBER('N'),
    PAYEE('P'),
    ADDRESS('A'),
    CATEGORY('L'),
    DATE('D'),
    AMOUNT('T'),
    AMOUNT_DUPLICATE('U'),
    CLEARED('C'),
    MEMO('M'),
    TRANSFER_ACCOUNT('L'),
    SPLIT_CATEGORY('S'),
    SPLIT_MEMO('E'),
    SPLIT_AMOUNT('$'),
    SECURITY_ACTION('N'),
    SECURITY_SYMBOL('S'),
    SECURITY('Y'),
    SECURITY_GOAL('G'),
    PRICE('I'),
    SHARES('Q'),
    COMMISSION('O'),
    CREDIT_LIMIT('L'),
    BALANCE('$'),
    BALANCE_DATE('/'),
    END(QuickenRecord.END);

    private char code;

    private QifField(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static QifField fromCode(char code) {
        for (QifField field : values()) {
            if (field.code == code) return field;
        }
        return null;
    }
}
