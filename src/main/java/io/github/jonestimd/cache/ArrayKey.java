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

import java.lang.reflect.Array;

import com.google.common.base.Joiner;

public class ArrayKey {
    private Object[] array;

    public ArrayKey(Object key) {
        if (key != null) {
            int length = Array.getLength(key);
            this.array = new Object[length];
            for (int i=0; i<length; i++) {
                Object item = Array.get(key, i);
                if (item != null && item.getClass().isArray()) {
                    this.array[i] = new ArrayKey(item);
                }
                else {
                    this.array[i] = item;
                }
            }
        }
    }

    public int hashCode() {
        int hash = 0;
        if (array != null) {
            for (Object obj : array) {
                if (obj != null) hash = hash * 29 + obj.hashCode();
            }
        }
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return array == null;
        }
        if (array != null && obj instanceof ArrayKey) {
            ArrayKey key = (ArrayKey) obj;
            boolean equal = array.length == key.array.length;
            for (int i=0; i<array.length && equal; i++) {
                if (array[i] == null) {
                    equal = key.array[i] == null;
                }
                else {
                    equal = array[i].equals(key.array[i]);
                }
            }
            return equal;
        }
        return false;
    }

    public String toString() {
        return array == null ? "null" :'[' + Joiner.on(",").join(array) + ']';
    }
}
