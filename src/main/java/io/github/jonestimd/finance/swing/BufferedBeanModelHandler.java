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
package io.github.jonestimd.finance.swing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javassist.util.proxy.MethodHandler;

/**
 * Method handler for {@link BufferedBeanModel}.
 */
public class BufferedBeanModelHandler<D> implements MethodHandler {
    public static final String CHANGED_PROPERTY = "changed";
    private static final Class[] ADD_PROP_LISTENER_PARAMS = new Class[] {String.class, PropertyChangeListener.class};
    private static final Class[] ADD_LISTENER_PARAMS = new Class[] {PropertyChangeListener.class};

    protected final D delegate;
    private final Map<String, Object> changeValues = new HashMap<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public BufferedBeanModelHandler(D delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        String methodName = thisMethod.getName();
        if (methodName.equals("isChanged") && args.length == 0) return isChanged();
        if (methodName.equals("addPropertyChangeListener")) {
            if (Arrays.equals(thisMethod.getParameterTypes(), ADD_PROP_LISTENER_PARAMS)) {
                changeSupport.addPropertyChangeListener((String) args[0], (PropertyChangeListener) args[1]);
            }
            if (Arrays.equals(thisMethod.getParameterTypes(), ADD_LISTENER_PARAMS)) {
                changeSupport.addPropertyChangeListener((PropertyChangeListener) args[0]);
            }
            return null;
        }
        if (methodName.equals("resetChanges")) {
            resetChanges();
            return null;
        }
        if (isGetter(thisMethod)) {
            String property = methodName.replaceAll("^(is|get)", "");
            return changeValues.getOrDefault(property, thisMethod.invoke(delegate, args));
        }
        if (isSetter(thisMethod)) {
            String property = methodName.substring(3);
            boolean oldChanged = isChanged();
            if (Objects.equals(args[0], getBeanValue(thisMethod))) changeValues.remove(property);
            else changeValues.put(property, args[0]);
            firePropertyChange(CHANGED_PROPERTY, oldChanged, isChanged());
            firePropertyChange(CHANGED_PROPERTY + property, null, changeValues.containsKey(property));
            return null;
        }
        if (thisMethod.getDeclaringClass().equals(self.getClass())) return proceed.invoke(self, args);
        return thisMethod.invoke(delegate, args);
    }

    protected boolean isChanged() {
        return !changeValues.isEmpty();
    }

    protected void resetChanges() {
        changeValues.clear();
    }

    protected Object getBeanValue(Method setter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String prefix = (setter.getParameterTypes()[0].equals(boolean.class)) ? "is" : "get";
        Method getter = setter.getDeclaringClass().getMethod(prefix + setter.getName().substring(3));
        return getter.invoke(delegate);
    }

    protected boolean isGetter(Method method) {
        return method.getParameterCount() == 0 && (
                method.getName().startsWith("get") || method.getName().startsWith("is") && method.getReturnType().equals(boolean.class));
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterCount() == 1;
    }

    protected void firePropertyChange(String property, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(property, oldValue, newValue);
    }
}
