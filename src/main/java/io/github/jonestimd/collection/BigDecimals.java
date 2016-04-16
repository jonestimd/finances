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
package io.github.jonestimd.collection;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Stream utilities for {@link BigDecimal}.
 */
public class BigDecimals {
    private BigDecimals() {}

    /**
     * @param adapter a function to extract a {@link BigDecimal} value from {@code  items}
     * @return the sum of the values extracted from the items
     */
    public static <T> BigDecimal sum(Collection<T> items, Function<? super T, BigDecimal> adapter) {
        return sum(items.stream().map(adapter));
    }

    /**
     * @return the sum of {@code items}
     */
    public static BigDecimal sum(Collection<BigDecimal> items) {
        return sum(items.stream());
    }

    /**
     * @return the sum of {@code items}
     */
    public static BigDecimal sum(Stream<BigDecimal> items) {
        return items.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}