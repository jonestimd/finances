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
package io.github.jonestimd.hibernate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class InterceptorChain extends EmptyInterceptor {
    private final List<Interceptor> chain;
    
    public InterceptorChain(Interceptor ... chain) {
        this.chain = Arrays.asList(chain);
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
            String[] propertyNames, Type[] types) throws CallbackException {
        boolean modified = false;
        for (Interceptor interceptor : chain) {
            modified |= interceptor.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        }
        return modified;
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        boolean modified = false;
        for (Interceptor interceptor : chain) {
            modified |= interceptor.onSave(entity, id, state, propertyNames, types);
        }
        return modified;
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        for (Interceptor interceptor : chain) {
            interceptor.onDelete(entity, id, state, propertyNames, types);
        }
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        for (Interceptor interceptor : chain) {
            interceptor.afterTransactionBegin(tx);
        }
    }

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        for (Interceptor interceptor : chain) {
            interceptor.afterTransactionCompletion(tx);
        }
    }
}