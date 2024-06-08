// The MIT License (MIT)
//
// Copyright (c) 2024 Tim Jones
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

public class QifExport {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd''yy");
    private final PrintWriter writer;
    
    public QifExport(Writer writer) {
        this.writer = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
    }

    public QifExport accountType(QifAccountType type) throws IOException {
        writer.append(QifField.CONTROL.code()).append("Type:").println(type.name());
        return this;
    }

    public QifExport account(String name) throws IOException {
        writer.append(QifField.CONTROL.code()).println("Account");
        writer.append(QifField.NAME.code()).println(name);
//        writer.append(QifField.TYPE.code()).println(QifAccountType.Bank.name());
        writer.println(QifField.END.code());
        return this;
    }

    public QifExport transaction(Transaction transaction) throws IOException {
        writer.append(QifField.DATE.code()).println(dateFormat.format(transaction.getDate()));
        writer.append(QifField.AMOUNT.code()).println(transaction.getAmount().toString());
        if (transaction.isCleared()) {
            writer.append(QifField.CLEARED.code()).println("X");
        }
        if (transaction.getPayee() != null) {
            writer.append(QifField.PAYEE.code()).println(transaction.getPayee().getName());
        }
        firstDetail(transaction.getDetails().get(0));
        if (transaction.getDetails().size() > 1) {
            for (TransactionDetail detail : transaction.getDetails()) {
                splitDetail(detail);
            }
        }
        writer.println(QifField.END.code());
        return this;
    }

    private void firstDetail(TransactionDetail detail) throws IOException {
        writer.append(QifField.CATEGORY.code()).println(formatCategory(detail));
        if (detail.getMemo() != null) {
            writer.append(QifField.MEMO.code()).println(detail.getMemo());
        }
    }
    
    private void splitDetail(TransactionDetail detail) throws IOException {
        writer.append(QifField.SPLIT_CATEGORY.code()).println(formatCategory(detail));
        if (detail.getMemo() != null) {
            writer.append(QifField.SPLIT_MEMO.code()).println(detail.getMemo());
        }
        writer.append(QifField.SPLIT_AMOUNT.code()).println(detail.getAmount() == null ? "0" : detail.getAmount().toString());
    }

    private String formatCategory(TransactionDetail detail) {
        if (detail.isTransfer()) {
            return String.format("[%s]", detail.getRelatedDetail().getTransaction().getAccount().getName());
        }
        return detail.getCategory().qualifiedName(":");
    }
}
