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

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import io.github.jonestimd.swing.ComponentTreeUtils;

import static io.github.jonestimd.finance.swing.BundleType.*;

public abstract class ValidatedTabPanel extends JPanel {
    private static final Color ERROR_COLOR = LABELS.getColor("error.foreground.color");
    private JTabbedPane tabbedPane;
    private int tabIndex = -1;

    public ValidatedTabPanel() {
    }

    public ValidatedTabPanel(LayoutManager layout) {
        super(layout);
    }

    protected abstract boolean isNoErrors();

    protected void setTabForeground() {
        if (tabbedPane != null) tabbedPane.setForegroundAt(tabIndex, isNoErrors() ? null : ERROR_COLOR);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        tabbedPane = ComponentTreeUtils.findAncestor(this, JTabbedPane.class);
        tabIndex = tabbedPane.indexOfComponent(this);
    }
}
