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
package io.github.jonestimd.finance.file.quicken.handler;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.qif.CategoryParser;
import io.github.jonestimd.finance.file.quicken.qif.QifField;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;

public class MiscSecurityDetailHandler implements SecurityDetailHandler {

    private final TransactionCategoryOperations TransactionCategoryOperations;
    private final TransactionGroupOperations transactionGroupOperations;
    private final boolean income;
    private final String defaultCategory;

    public MiscSecurityDetailHandler(TransactionCategoryOperations txTypeOperations, TransactionGroupOperations txGroupOperations,
                                     boolean income, String defaultCategory) {
        this.TransactionCategoryOperations = txTypeOperations;
        this.transactionGroupOperations = txGroupOperations;
        this.income = income;
        this.defaultCategory = defaultCategory;
    }

    public void addDetails(QifRecord record, Transaction transaction, boolean isSharesOut) throws QuickenException {
        String category = record.getValue(QifField.CATEGORY);
        TransactionCategory type = null;
        TransactionGroup group = null;
        if (category != null) {
            CategoryParser parser = new CategoryParser(category);
            group = getGroup(parser);
            if (parser.isTransfer()) {
                throw new QuickenException("invalidCategory", category, record.getStartingLine());
            }
            type = getTransactionCategory(parser);
        }
        if (type == null) {
            type = TransactionCategoryOperations.getOrCreateTransactionCategory(null, income, defaultCategory);
        }
        BigDecimal amount = record.getBigDecimal(QifField.AMOUNT);
        String memo = record.getValue(QifField.MEMO);
        transaction.addDetails(new TransactionDetail(type, income ? amount : amount.negate(), memo, group));
    }

    private TransactionGroup getGroup(CategoryParser parser) {
        return parser.hasGroup() ? transactionGroupOperations.getTransactionGroup(parser.getGroupName()) : null;
    }

    private TransactionCategory getTransactionCategory(CategoryParser parser) {
        return parser.hasCatetories() ? TransactionCategoryOperations.getTransactionCategory(parser.getCategoryNames()) : null;
    }
}
