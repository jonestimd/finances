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
import java.util.Date;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class AuditInterceptor extends EmptyInterceptor {
    private ThreadLocal<Date> changeDateHolder = new ThreadLocal<Date>();
    private ThreadLocal<String> changeUserHolder = new ThreadLocal<String>();

    public void afterTransactionBegin(Transaction tx) {
        super.afterTransactionBegin(tx);
        changeDateHolder.set(new Date());
        changeUserHolder.set(System.getProperty("user.name", "unknown"));
    }

    public void afterTransactionCompletion(Transaction tx) {
        super.afterTransactionCompletion(tx);
        changeDateHolder.set(null);
        changeUserHolder.set(null);
    }

    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
            String[] propertyNames, Type[] types) {
        boolean modified = super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        return setProperties(currentState, propertyNames, types) || modified;
    }

    private boolean setProperties(Object[] state, String[] propertyNames, Type[] types) {
        boolean modified = setProperty(state, propertyNames, types, "changeDate", changeDateHolder.get());
        return setProperty(state, propertyNames, types, "changeUser", changeUserHolder.get()) || modified;
    }

    private boolean setProperty(Object[] state, String[] propertyNames, Type[] types, String propertyName, Object value) {
        int index = Arrays.asList(propertyNames).indexOf(propertyName);
        if (index >= 0) {
            state[index] = value;
            return true;
        }
        return false;
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        boolean modified = super.onSave(entity, id, state, propertyNames, types);
        return setProperties(state, propertyNames, types) || modified;
    }
}
