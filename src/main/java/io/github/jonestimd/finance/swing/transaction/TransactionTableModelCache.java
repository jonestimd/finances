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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.swing.asset.SecurityTransactionTableModel;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;

/**
 * This class is not thread-safe.  It is intendend for use only on the AWT event thread.
 */
public class TransactionTableModelCache {
    private DomainEventPublisher eventPublisher;
    private ReferenceQueue<TransactionTableModel> queue = new ReferenceQueue<>();
    private Map<Long, WeakReference<TransactionTableModel>> models = new HashMap<>();

    public TransactionTableModelCache(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public TransactionTableModel getModel(Account account) {
        expungeStaleEntries();
        TransactionTableModel model = findModel(account.getId());
        return model == null ? createModel(account) : model;
    }

    private TransactionTableModel findModel(Long accountId) {
        WeakReference<TransactionTableModel> weakReference = models.get(accountId);
        return weakReference == null ? null : weakReference.get();
    }

    private TransactionTableModel createModel(Account account) { // TODO use CurrencyTransactionTableModel
        TransactionTableModel model = account.getType().isSecurity()
            ? new SecurityTransactionTableModel(account) : new TransactionTableModel(account);
        model.setDomainEventPublisher(eventPublisher);
        models.put(account.getId(), new WeakReference<>(model, queue));
        return model;
    }

    private void expungeStaleEntries() {
        Reference<?> ref;
        while ((ref = queue.poll()) != null) {
            models.values().remove(ref);
        }
    }
}