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
package io.github.jonestimd.finance.swing;

import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import io.github.jonestimd.swing.action.CancelAction;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.dialog.MessageDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;

public class AboutDialog extends MessageDialog {
    public AboutDialog(Window owner) {
        super(owner, BundleType.LABELS.getString("dialog.about.title"), ModalityType.APPLICATION_MODAL);
        GridBagBuilder builder = new GridBagBuilder(getContentPane(), BundleType.LABELS.get(), "dialog.about.");
        builder.append("version.label", TextField.plain().readOnly().get()).setText(BundleType.LABELS.getString("dialog.about.version.value"));
        builder.append("buildDate.label", TextField.plain().readOnly().get()).setText(BundleType.LABELS.getString("dialog.about.buildDate.value"));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(5, 5, 5, 5));
        CancelAction.install(this);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            pack();
            setLocationRelativeTo(getOwner());
        }
        super.setVisible(visible);
    }
}
