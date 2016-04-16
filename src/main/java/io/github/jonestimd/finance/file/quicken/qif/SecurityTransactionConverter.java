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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.handler.SecurityTransactionHandler;
import io.github.jonestimd.finance.service.TransactionService;

public class SecurityTransactionConverter implements RecordConverter {

    private static final String RECORD_TYPE = "Type:Invst";

    private TransactionService transactionService;
    private Map<String, SecurityTransactionHandler> actionHandlers = new HashMap<String, SecurityTransactionHandler>();

    public SecurityTransactionConverter(TransactionService transactionService, Map<String, SecurityTransactionHandler> actionHandlers) {
        this.transactionService = transactionService;
        this.actionHandlers = actionHandlers;
    }

    public Set<String> getTypes() {
        return Collections.singleton(RECORD_TYPE);
    }

    public void importRecord(AccountHolder accountHolder, QifRecord record) throws QuickenException {
        String action = record.getValue(QifField.SECURITY_ACTION);
        SecurityTransactionHandler handler = actionHandlers.get(action);
        if (handler == null) {
            throw new QuickenException("unknownSecurityAction", action, record.getStartingLine());
        }
        List<Transaction> transactions = handler.convertRecord(accountHolder.getAccount(), record);
        transactionService.saveTransactions(transactions);
    }
}