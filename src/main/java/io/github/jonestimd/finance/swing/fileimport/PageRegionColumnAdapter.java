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

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class PageRegionColumnAdapter<V> extends FunctionColumnAdapter<PageRegion, V> {
    public static final String RESOURCE_PREFIX = "table.importPageRegion.column.";

    protected PageRegionColumnAdapter(String columnId, Class<? super V> valueType, Function<PageRegion, V> getter, BiConsumer<PageRegion, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, getter, setter);
    }

    public static final PageRegionColumnAdapter<String> NAME_ADAPTER =
        new PageRegionColumnAdapter<>("name", String.class, PageRegion::getName, PageRegion::setName);

    public static final PageRegionColumnAdapter<Float> TOP_ADAPTER =
        new PageRegionColumnAdapter<>("top", Float.class, PageRegion::getTop, PageRegion::setTop);

    public static final PageRegionColumnAdapter<Float> BOTTOM_ADAPTER =
        new PageRegionColumnAdapter<>("bottom", Float.class, PageRegion::getBottom, PageRegion::setBottom);

    public static final PageRegionColumnAdapter<Float> LABEL_LEFT_ADAPTER =
        new PageRegionColumnAdapter<>("labelLeft", Float.class, PageRegion::getLabelLeft, PageRegion::setLabelLeft);

    public static final PageRegionColumnAdapter<Float> LABEL_RIGHT_ADAPTER =
        new PageRegionColumnAdapter<>("labelRight", Float.class, PageRegion::getLabelRight, PageRegion::setLabelRight);

    public static final PageRegionColumnAdapter<Float> VALUE_LEFT_ADAPTER =
        new PageRegionColumnAdapter<>("valueLeft", Float.class, PageRegion::getValueLeft, PageRegion::setValueLeft);

    public static final PageRegionColumnAdapter<Float> VALUE_RIGHT_ADAPTER =
        new PageRegionColumnAdapter<>("valueRight", Float.class, PageRegion::getValueRight, PageRegion::setValueRight);
}
