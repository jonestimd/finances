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
package io.github.jonestimd.finance.yahoo;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

public class QueryBuilder {
    private final StringBuilder query = new StringBuilder();
    private boolean where = true;
    private String conditionColumn;

    public QueryBuilder(JsonMapper<?> entity) {
        query.append("select ").append(StringUtils.join(entity.getColumns().iterator(), ","))
                .append(" from ").append(entity.getTable());

    }

    public QueryBuilder where(String column) {
        conditionColumn = column;
        return this;
    }

    public QueryBuilder in(Collection<String> values) {
        if (! values.isEmpty()) {
            query.append(where ? " where " : " and ").append(conditionColumn)
                    .append(" in (\"").append(StringUtils.join(values, "\",\"")).append("\")");
            where = false;
        }
        return this;
    }

    public String get() {
        return query.toString();
    }
}
