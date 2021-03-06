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
package io.github.jonestimd.finance.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableCellRenderer;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;

public interface FinancePlugin {
    void initialize(Config config, ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher);

    List<? extends SecurityTableExtension> getSecurityTableExtensions();

    /**
     * Get value type renderers.
     * @return a map of value class to renderer.
     */
    default Map<Class<?>, TableCellRenderer> getTableCellRenderers() {
        return Collections.emptyMap();
    }

    /**
     * Get column specific table cell renderers.
     * @return a map of column ID to renderer.
     */
    default Map<String, TableCellRenderer> getTableColumnRenderers() {
        return Collections.emptyMap();
    }
}
