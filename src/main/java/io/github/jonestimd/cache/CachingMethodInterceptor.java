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
package io.github.jonestimd.cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.log4j.Logger;

public class CachingMethodInterceptor implements InvocationHandler {
    private Logger logger = Logger.getLogger(CachingMethodInterceptor.class);
    private Object target;
    @SuppressWarnings("unchecked")
    private Cache<ArrayKey, Object> cache = CacheBuilder.newBuilder().softValues().build();

    public CachingMethodInterceptor(Object target) {
        this.target = target;
    }

    private Object[] getKeyParameters(Method method, Object[] args) {
        int[] keyParameters = getKeyParameterIndexes(method);
        if (keyParameters.length == 0) {
            return args;
        }
        Object[] keys = new Object[keyParameters.length];
        for (int i=0; i<keyParameters.length; i++) {
            keys[i] = args[keyParameters[i]];
        }
        return keys;
    }

    private int[] getKeyParameterIndexes(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        int[] indexes = new int[annotations.length];
        int j = 0;
        for (int i = 0; i < annotations.length; i++) {
            if (hasCacheKey(annotations[i])) {
                indexes[j++] = i;
            }
        }
        return Arrays.copyOfRange(indexes, 0, j);
    }

    private boolean hasCacheKey(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(CacheKey.class)) {
                return true;
            }
        }
        return false;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method cacheableMethod = getCacheableMethod(method);
        if (! cacheableMethod.isAnnotationPresent(Cacheable.class)) {
            return method.invoke(target, args);
        }
        ArrayKey key = new ArrayKey(getKeyParameters(cacheableMethod, args));
        Object result = cache.getIfPresent(key);
        if (result == null) {
            result = method.invoke(target, args);
            if (result != null) cache.put(key, result);
        }
        return result;
    }

    private Method getCacheableMethod(Method method) {
        if (! method.isAnnotationPresent(Cacheable.class)) {
            try {
                return target.getClass().getMethod(method.getName(), method.getParameterTypes());
            }
            catch (Exception ex) {
                logger.warn("error getting annotation", ex);
            }
        }
        return method;
    }
}