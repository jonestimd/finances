// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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
package io.github.jonestimd.finance.swing.fileimport;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.jonestimd.finance.domain.fileimport.ImportField;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class ImportFieldModel extends ImportField {
    public static final String CHANGED_PROPERTY = "changed";
    private static final ProxyFactory factory;

    static {
        factory = new ProxyFactory();
        factory.setSuperclass(ImportFieldModel.class);
    }

    private final Map<String, Object> originalValues = new HashMap<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public static ImportFieldModel create(ImportField importField) {
        try {
            return (ImportFieldModel) factory.create(new Class[0], new Object[0], new Handler(importField));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ImportFieldModel() {
    }

    public boolean isChanged() {
        return !originalValues.isEmpty();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    protected void firePropertyChange(String property, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(property, oldValue, newValue);
    }

    private static class Handler implements MethodHandler {
        private final ImportField delegate;

        private Handler(ImportField delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            if (thisMethod.getName().startsWith("set") && thisMethod.getParameterCount() == 1) {
                ImportFieldModel model = (ImportFieldModel) self;
                String property = thisMethod.getName().substring(3);
                if (model.originalValues.containsKey(property)) {
                    if (Objects.equals(args[0], model.originalValues.get(property))) {
                        model.originalValues.remove(property);
                        if (!model.isChanged()) model.firePropertyChange("changed", true, false);
                    }
                }
                else {
                    String prefix = (thisMethod.getParameterTypes()[0].equals(boolean.class)) ? "is" : "get";
                    Method getter = ImportField.class.getMethod(prefix + property);
                    Object originalValue = getter.invoke(delegate);
                    if (!Objects.equals(originalValue, args[0])) {
                        boolean oldChanged = model.isChanged();
                        model.originalValues.put(property, originalValue);
                        model.firePropertyChange(CHANGED_PROPERTY, oldChanged, true);
                    }
                }
            }
            if (thisMethod.getDeclaringClass().equals(ImportFieldModel.class)) return proceed.invoke(self, args);
            return thisMethod.invoke(delegate, args);
        }
    }
}
