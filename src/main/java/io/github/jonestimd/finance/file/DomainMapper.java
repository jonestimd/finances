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
package io.github.jonestimd.finance.file;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class DomainMapper<T> {
    private final Collection<T> targets;
    private final Function<T, String> upperNameFunction;
    private final Map<String, T> aliasMap;
    private final Function<String, T> factory;
    private final Comparator<T> nameLengthComparator;

    public DomainMapper(Collection<T> targets, Function<T, String> nameFunction, Map<String, T> aliasMap, Function<String, T> factory) {
        this.targets = targets;
        this.upperNameFunction = nameFunction.andThen(String::toUpperCase);
        this.aliasMap = aliasMap;
        this.factory = factory;
        nameLengthComparator = (target1, target2) -> Integer.compare(nameFunction.apply(target1).length(), nameFunction.apply(target2).length());
    }

    public T get(String name) {
        return Optional.ofNullable(aliasMap.get(name)).orElse(findOrCreate(name));
    }

    private T findOrCreate(String name) {
        Optional<T> match = targets.stream().filter(matches(name)).max(nameLengthComparator);
        return match.isPresent() ? match.get() : addTarget(name);
    }

    private Predicate<T> matches(String name) {
        return target -> name.toUpperCase().contains(upperNameFunction.apply(target));
    }

    private T addTarget(String name) {
        if (factory != null) {
            T target = factory.apply(name);
            targets.add(target);
            return target;
        }
        return null;
    }
}
