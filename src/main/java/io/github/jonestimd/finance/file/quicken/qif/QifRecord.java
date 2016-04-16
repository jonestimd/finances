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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.QuickenRecord;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;


public class QifRecord extends QuickenRecord {
    private static final List<Character> SPLIT_CODES = Arrays.asList(new Character[] {
        SPLIT_AMOUNT.code(), SPLIT_CATEGORY.code(), SPLIT_MEMO.code(),
    });

    private int lines = 0;
    private List<String> categories;
    private List<String> memos;
    private List<BigDecimal> amounts;
    private Map<String, BigDecimal> categoryAmounts;

    public QifRecord(long startingLine) {
        super(startingLine);
    }

    public Date getDate(QifField field) throws QuickenException {
        return getDate(field.code());
    }

    public BigDecimal getBigDecimal(QifField field) {
        return getBigDecimal(field.code());
    }

    public List<BigDecimal> getBigDecimals(QifField field) {
        return getBigDecimals(field.code());
    }

    public String getValue(QifField field) {
        return getValue(field.code());
    }

    public List<String> getValues(QifField field) {
        return getValues(field.code());
    }

    public boolean hasValue(QifField field) {
        return hasValue(field.code());
    }

    public void setValue(QifField field, String value) {
        setValue(field.code(), value);
    }

    public void setValue(char code, String value) {
        lines++;
        if (SPLIT_CODES.contains(code)) {
            if (hasValue(SPLIT_AMOUNT) && code != SPLIT_AMOUNT.code()) {
                int splitCount = getValues(SPLIT_AMOUNT).size();
                int count = hasValue(code) ? getValues(code).size() : 0;
                for (int i=count; i<splitCount; i++) {
                    super.setValue(code, "");
                }
            }
        }
        super.setValue(code, value);
    }

    public int getLines() {
        return lines;
    }

    public boolean isOption() {
        List<String> values = getValues(CONTROL);
        return ! values.isEmpty() && (values.get(0).matches("(Option|Clear).*"));
    }

    public boolean isControl() {
        return hasValue(CONTROL);
    }

    public String getControlValue() {
        return getValues(CONTROL).get(0).trim();
    }

    public boolean isComplete() {
        return super.isComplete() || hasValue(CONTROL);
    }

    public boolean isValid() {
        return super.isValid() || size() == 1 && hasValue(CONTROL);
    }

    public boolean isSplit() {
        return getValues(SPLIT_AMOUNT).size() > 1;
    }

    public int getDetailCount() {
        return isSplit() ? getValues(SPLIT_AMOUNT).size() : 1;
    }

    public String getCategory(int index) {
        if (categories == null) {
            categories = isSplit() ? getValues(SPLIT_CATEGORY) : Collections.singletonList(getValue(CATEGORY));
        }
        return index < categories.size() ? categories.get(index) : null;
    }

    public String getMemo(int index) {
        if (memos== null) {
            memos = isSplit() ? getValues(SPLIT_MEMO) : Collections.singletonList(getValue(MEMO));
        }
        return index < memos.size() ? memos.get(index) : null;
    }

    public BigDecimal getAmount(int index) {
        if (amounts == null) {
            amounts = isSplit() ? getBigDecimals(SPLIT_AMOUNT) : Collections.singletonList(getBigDecimal(AMOUNT));
        }
        return amounts.get(index);
    }

    public BigDecimal getCategoryAmount(int index) {
        if (categoryAmounts == null) {
            categoryAmounts = new HashMap<String, BigDecimal>();
            for (int i = 0; i < getDetailCount(); i++) {
                String category = getCategory(i);
                BigDecimal total = categoryAmounts.get(category);
                categoryAmounts.put(category, total == null ? getAmount(i) : total.add(getAmount(i)));
            }
        }
        return categoryAmounts.get(getCategory(index));
    }

    public Transaction createTransaction(Account account, Payee payee, TransactionDetail ... details) throws QuickenException {
        Transaction transaction = new Transaction(account, getDate(DATE), payee, hasValue(CLEARED), getValue(MEMO), details);
        if (! account.getType().isSecurity()) {
            transaction.setNumber(getValue(NUMBER));
        }
        return transaction;
    }

    public Transaction createSecurityTransaction(Account account) throws QuickenException {
        return new Transaction(account, getDate(DATE), hasValue(CLEARED), getValue(MEMO));
    }
}
