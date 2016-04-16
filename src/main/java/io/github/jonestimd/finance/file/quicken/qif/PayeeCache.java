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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.util.Streams;

public class PayeeCache implements PayeeOperations {
    private static final Function<Payee, String> GET_KEY = payee -> payee.getName().toUpperCase();
    private PayeeOperations delegate;
    private Map<String, Payee> cache = new HashMap<>(); // TODO free memory

    public PayeeCache(PayeeOperations delegate) {
        this.delegate = delegate;
        List<Payee> payees = delegate.getAllPayees();
        for (Payee payee : payees) {
            cache.put(GET_KEY.apply(payee), payee);
        }
    }

    public Payee createPayee(String name) {
        Payee payee = delegate.createPayee(name);
        cache.put(GET_KEY.apply(payee), payee);
        return payee;
    }

    public List<Payee> getAllPayees() {
        return new ArrayList<>(cache.values());
    }

    public List<PayeeSummary> getPayeeSummaries() {
        return delegate.getPayeeSummaries();
    }

    public Payee getPayee(String name) {
        return cache.get(name.toUpperCase());
    }

    public <T extends Iterable<Payee>> T saveAll(T payees) {
        return delegate.saveAll(payees);
    }

    @Override
    public <T extends Iterable<Payee>> void deleteAll(T payees) {
        delegate.deleteAll(payees);
        cache.keySet().removeAll(Streams.map(payees, GET_KEY));
    }

    @Override
    public List<Payee> merge(List<Payee> toReplace, Payee payee) {
        List<Payee> deleted = delegate.merge(toReplace, payee);
        cache.keySet().removeAll(Streams.map(deleted, GET_KEY));
        return deleted;
    }
}