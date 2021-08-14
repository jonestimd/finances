// The MIT License (MIT)
//
// Copyright (c) 2021 Tim Jones
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class TransactionInterceptor implements InvocationHandler {
    private final Object target;
    private final SessionFactory sessionFactory;
    private static final ThreadLocal<Transaction> transactionHolder = new ThreadLocal<>();

    public TransactionInterceptor(Object target, SessionFactory sessionFactory) {
        this.target = target;
        this.sessionFactory = sessionFactory;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (transactionHolder.get() == null) {
            Session session = sessionFactory.getCurrentSession();
            Transaction transaction = session.getTransaction();
            if (! (transaction.isActive())) {
                try {
                    transaction.begin();
                    transactionHolder.set(transaction);
                    Object result = method.invoke(target, args);
                    transaction.commit();
                    return result;
                }
                catch (InvocationTargetException ex) {
                    transaction.rollback();
                    throw ex.getTargetException();
                }
                catch (Throwable ex) {
                    transaction.rollback();
                    throw ex;
                }
                finally {
                    transactionHolder.set(null);
                    if (session.isOpen()) {
                        session.close();
                    }
                }
            }
        }
        return method.invoke(target, args);
    }
}