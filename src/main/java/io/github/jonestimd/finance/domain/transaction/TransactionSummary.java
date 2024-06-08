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
package io.github.jonestimd.finance.domain.transaction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.util.Streams;

public class TransactionSummary<T extends Comparable<? super T> & UniqueId<Long>> implements Comparable<TransactionSummary<T>>, UniqueId<Long> {
    public static final String TRANSACTION_COUNT = "transactionCount";

    public static <S extends Comparable<? super S> & UniqueId<Long>, T extends TransactionSummary<S>, V> Function<T, V> compose(Function<S, V> function) {
        return function.compose(TransactionSummary::getTransactionAttribute);
    }

    public static <S extends Comparable<? super S> & UniqueId<Long>, T extends TransactionSummary<S>, V> BiConsumer<T, V> compose(BiConsumer<S, V> consumer) {
        return (summary, value) -> consumer.accept(summary.getTransactionAttribute(), value);
    }

    public static <T extends Comparable<? super T> & UniqueId<Long>> Predicate<TransactionSummary<T>> isSummary(T attribute) {
        return summary -> attribute.getId().equals(summary.getId());
    }

    public static <T extends Comparable<? super T> & UniqueId<Long>, S extends TransactionSummary<T>> List<T> getAttributes(Iterable<S> summaries) {
        return Streams.map(summaries, TransactionSummary::getTransactionAttribute);
    }

    private T transactionAttribute;
    private long transactionCount;

    public TransactionSummary(T transactionAttribute, long transactionCount) {
        this.transactionAttribute = transactionAttribute;
        this.transactionCount = transactionCount;
    }

    @Override
    public Long getId() {
        return transactionAttribute.getId();
    }

    public T getTransactionAttribute() {
        return transactionAttribute;
    }

    protected void setTransactionAttribute(T transactionAttribute) {
        this.transactionAttribute = transactionAttribute;
    }

    public boolean isUsed() {
        return transactionCount > 0;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public void addToTransactionCount(long count) {
        this.transactionCount += count;
    }

    public int compareTo(TransactionSummary<T> that) {
        return transactionAttribute.compareTo(that.transactionAttribute);
    }

    public String toString() {
        return transactionAttribute.toString() + " (" + transactionCount + ")";
    }
}