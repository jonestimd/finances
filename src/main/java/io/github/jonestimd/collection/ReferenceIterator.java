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

import java.lang.ref.Reference;
import java.util.Collection;
import java.util.Iterator;

/**
 * Wrapper for an {@link Iterator} of {@link Reference} that removes items that have been garbage collected.
 * @param <R>
 */
public class ReferenceIterator<R> implements Iterator<R> {
    private Iterator<? extends Reference<R>> delegate;
    private R next;

    public ReferenceIterator(Iterator<? extends Reference<R>> delegate) {
        this.delegate = delegate;
    }

    public boolean hasNext() {
        next = null;
        while (delegate.hasNext() && next == null) {
            next = delegate.next().get();
            if (next == null) {
                delegate.remove();
            }
        }
        return next != null;
    }

    public R next() {
        return next;
    }

    public void remove() {
        delegate.remove();
    }

    public static <T> ReferenceIterator<T> iterator(Collection<? extends Reference<T>> collection) {
        return new ReferenceIterator<T>(collection.iterator());
    }
}