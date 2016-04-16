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
package io.github.jonestimd.finance.swing.transaction;

import java.util.function.Predicate;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;

public class TransactionFilter implements Predicate<Transaction> {
    private final String searchText;

    public TransactionFilter(String searchText) {
        this.searchText = searchText;
    }

    public boolean test(Transaction transaction) {
        return searchText.isEmpty() || matchesTransaction(transaction, searchText.toLowerCase());
    }

    private boolean matchesTransaction(Transaction transaction, String filterText) {
        return transaction.isNew() || transaction.getMemo() != null && transaction.getMemo().toLowerCase().contains(filterText)
                || matchesPayee(transaction.getPayee(), filterText) || matchesSecurity(transaction.getSecurity(), filterText)
                || transaction.getAmount().toString().contains(filterText)
                || matchesDetail(transaction, filterText);
    }

    private boolean matchesPayee(Payee payee, String filterText) {
        return payee != null && payee.getName().toLowerCase().contains(filterText);
    }

    private boolean matchesSecurity(Security security, String filterText) {
        return security != null && security.getName().toLowerCase().contains(filterText);
    }

    private boolean matchesDetail(Transaction transaction, String filterText) {
        return transaction.getDetails().stream().anyMatch(detail -> detail.getMemo() != null && detail.getMemo().toLowerCase().contains(filterText)
                || detail.getCategory() != null && detail.getCategory().qualifiedName(" ").toLowerCase().contains(filterText)
                || detail.getAmount().toString().contains(filterText));
    }
}
