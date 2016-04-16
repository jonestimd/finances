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
package io.github.jonestimd.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GenericsHelper {
    /**
     * Find the class for a generic parameter.
     * @param child class for which a type has been assigned for the parameter
     * @param ancestor generic class declaring the parameter
     * @param parameterName name of the parameter on {@code ancestor}
     * @return the class assigned to the generic parameter
     * @throws IllegalArgumentException if the parameter has not been assigned a type on the class hierarchy of {@code child}
     */
    public static Class<?> getParameterType(Class<?> child, Class<?> ancestor, String parameterName) {
        ClassHierarchy hierarchy = new ClassHierarchy(child, ancestor);
        int variableIndex = getParameterIndex(ancestor, parameterName);
        for (Class<?> aClass : hierarchy) {
            Type type = getParameterType(aClass, variableIndex);
            if (type instanceof TypeVariable) {
                // get name from class declaration
                TypeVariable var = (TypeVariable) type;
                variableIndex = getParameterIndex(aClass, var.getName());
            }
            else {
                return (Class<?>) type;
            }
        }
        throw new IllegalArgumentException(String.format("type not found for '%s'", parameterName));
    }

    public static Class<?> getInterfaceParameterType(Class<?> child, Class<?> parent, String parameterName) {
        for (Type type : child.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType superType = (ParameterizedType) type;
                if (superType.getRawType().equals(parent)) {
                    return (Class<?>) superType.getActualTypeArguments()[getParameterIndex(parent, parameterName)];
                }
            }
        }
        throw new IllegalArgumentException(String.format("type not found for '%s'", parameterName));
    }

    /**
     * Get the class or generic parameter assigned to a parameter of the generic super class.
     * @param subClass a subclass of a generic class
     * @param parameterIndex the index of the parameter on the generic super class
     * @return the type declared for the parameter on {@code subClass}
     */
    private static Type getParameterType(Class<?> subClass, int parameterIndex) {
        ParameterizedType superType = (ParameterizedType) subClass.getGenericSuperclass();
        return superType.getActualTypeArguments()[parameterIndex];
    }

    /**
     * Get the index of a parameter on a generic class.
     * @param aClass the generic class
     * @param name the name of the parameter
     * @return the index of the parameter
     * @throws IllegalArgumentException {@code aClass} does not have the specified generic parameter
     */
    private static int getParameterIndex(Class<?> aClass, String name) {
        TypeVariable<? extends Class<?>>[] typeParameters = aClass.getTypeParameters();
        for (int i = 0; i < typeParameters.length; i++) {
            TypeVariable<? extends Class<?>> typeVariable = typeParameters[i];
            if (typeVariable.getName().equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.format("no parameter named '%s'", name));
    }

    private static class ClassHierarchy implements Iterable<Class<?>> {
        private List<Class<?>> hierarchy = new LinkedList<Class<?>>();

        public ClassHierarchy(Class<?> child, Class<?> ancestor) {
            for (Class<?> parent = child; ! parent.equals(ancestor); parent = parent.getSuperclass()) {
                hierarchy.add(0, parent);
            }
        }

        @Override
        public Iterator<Class<?>> iterator() {
            return hierarchy.iterator();
        }
    }
}
