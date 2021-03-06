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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;

/**
 * Method handler for {@link BufferedBeanModel}.  To use, declare an abstract class like the following and
 * create {@link Proxy}s using this class as the {@link MethodHandler}.
 * <pre>
 * public abstract class Model extends Bean implements {@link BufferedBeanModel}<Bean> {
 *     protected abstract void firePropertyChange(String property, Object oldValue, Object newValue);
 * }
 * </pre>
 * The <code>firePropertyChange</code> method is optional.  It can be called within the <code>Model</code>
 * to fire property change events.  Any concrete methods declared on the <code>Model</code> will be used as is.
 * @param <D> the class of the delegate object (the <code>Bean</code> class)
 */
public class BufferedBeanModelHandler<D> implements MethodHandler {
    public static final String CHANGED_PROPERTY = "changed";
    private static final Class[] PROP_LISTENER_PARAMS = new Class[] {String.class, PropertyChangeListener.class};
    private static final Class[] LISTENER_PARAMS = new Class[] {PropertyChangeListener.class};
    private static final List<String> SELF_METHODS = ImmutableList.of("equals", "hashCode");

    protected final D delegate;
    private final Map<String, Object> changeValues = new HashMap<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public BufferedBeanModelHandler(D delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        if (isSelfMethod(self, thisMethod)) {
            return proceed.invoke(self, args);
        }
        if (isFirePropertyChange(thisMethod)) {
            firePropertyChange((String) args[0], args[1], args[2]);
            return null;
        }
        String methodName = thisMethod.getName();
        if (isGetter(thisMethod) && methodName.equals("getBean")) return delegate;
        if (methodName.equals("isChanged") && args.length == 0) return isChanged(self);
        if (methodName.equals("addPropertyChangeListener")) {
            if (Arrays.equals(thisMethod.getParameterTypes(), PROP_LISTENER_PARAMS)) {
                changeSupport.addPropertyChangeListener((String) args[0], (PropertyChangeListener) args[1]);
            }
            if (Arrays.equals(thisMethod.getParameterTypes(), LISTENER_PARAMS)) {
                changeSupport.addPropertyChangeListener((PropertyChangeListener) args[0]);
            }
            return null;
        }
        if (methodName.equals("removePropertyChangeListener")) {
            if (Arrays.equals(thisMethod.getParameterTypes(), PROP_LISTENER_PARAMS)) {
                changeSupport.removePropertyChangeListener((String) args[0], (PropertyChangeListener) args[1]);
            }
            if (Arrays.equals(thisMethod.getParameterTypes(), LISTENER_PARAMS)) {
                changeSupport.removePropertyChangeListener((PropertyChangeListener) args[0]);
            }
            return null;
        }
        if (methodName.equals("commitChanges")) {
            commitChanges(self);
            firePropertyChange(CHANGED_PROPERTY, null, false);
            return delegate;
        }
        if (methodName.equals("resetChanges")) {
            resetChanges(self);
            firePropertyChange(CHANGED_PROPERTY, null, false);
            return null;
        }
        if (isGetter(thisMethod)) {
            String property = methodName.replaceAll("^(is|get)", "");
            return changeValues.getOrDefault(property, thisMethod.invoke(delegate, args));
        }
        if (isSetter(thisMethod)) {
            String property = methodName.substring(3);
            boolean oldChanged = isChanged(self);
            Object originalValue = getBeanValue(thisMethod);
            Object oldValue = changeValues.getOrDefault(property, originalValue);
            if (Objects.equals(args[0], originalValue)) changeValues.remove(property);
            else changeValues.put(property, args[0]);
            firePropertyChange(CHANGED_PROPERTY, oldChanged, isChanged(self));
            firePropertyChange(normalCase(property), oldValue, args[0]);
            firePropertyChange(CHANGED_PROPERTY + property, null, changeValues.containsKey(property));
            return null;
        }
        return thisMethod.invoke(delegate, args);
    }

    private boolean isSelfMethod(Object self, Method thisMethod) {
        return thisMethod.getDeclaringClass().equals(self.getClass().getSuperclass()) && !Modifier.isAbstract(thisMethod.getModifiers())
                || SELF_METHODS.contains(thisMethod.getName());
    }

    private String normalCase(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private boolean isFirePropertyChange(Method method) {
        return method.getName().equals("firePropertyChange") && method.getParameterCount() == 3
                && method.getParameterTypes()[0].equals(String.class);
    }

    protected boolean isChanged(Object self) {
        return !changeValues.isEmpty();
    }

    protected void commitChanges(Object self) {
        for (Entry<String, Object> entry : changeValues.entrySet()) {
            setBeanValue(entry.getKey(), entry.getValue());
            firePropertyChange(CHANGED_PROPERTY + entry.getKey(), null, false);
        }
        changeValues.clear();
        firePropertyChange(CHANGED_PROPERTY, null, false);
    }

    protected void resetChanges(Object self) {
        Set<String> changedProps = new HashSet<>(changeValues.keySet());
        changeValues.clear();
        changedProps.forEach(prop -> {
            firePropertyChange(normalCase(prop), null, null);
            firePropertyChange(CHANGED_PROPERTY + prop, null, false);
        });
        firePropertyChange(CHANGED_PROPERTY, null, false);
    }

    protected Object getBeanValue(Method setter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String prefix = (setter.getParameterTypes()[0].equals(boolean.class)) ? "is" : "get";
        Method getter = setter.getDeclaringClass().getMethod(prefix + setter.getName().substring(3));
        return getter.invoke(delegate);
    }

    protected void setBeanValue(String property, Object value) {
        try {
            Predicate<Method> isMethod = (method) -> method.getName().equals("set" + property) && method.getParameterCount() == 1;
            Method setter = Arrays.stream(delegate.getClass().getMethods()).filter(isMethod).findFirst().get();
            setter.invoke(delegate, value);
            firePropertyChange(normalCase(property), null, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected boolean isGetter(Method method) {
        return method.getParameterCount() == 0 && (
                method.getName().startsWith("get") || method.getName().startsWith("is") && method.getReturnType().equals(boolean.class));
    }

    protected boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterCount() == 1;
    }

    protected void firePropertyChange(String property, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(property, oldValue, newValue);
    }
}
