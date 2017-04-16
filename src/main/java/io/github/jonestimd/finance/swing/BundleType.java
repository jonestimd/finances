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

import java.awt.Color;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.common.base.Supplier;
import io.github.jonestimd.swing.ColorFactory;

public enum BundleType implements Supplier<ResourceBundle> {
    LABELS("io.github.jonestimd.finance.ComponentLabels"),
    MESSAGES("io.github.jonestimd.finance.Messages"),
    REFERENCE("io.github.jonestimd.finance.ReferenceData"),
    QUICKEN_CAPITAL_GAIN("io.github.jonestimd.finance.file.quicken.CapitalGain");

    private final ResourceBundle bundle;

    private BundleType(String basename) {
        this.bundle = ResourceBundle.getBundle(basename);
    }

    public ResourceBundle get() {
        return bundle;
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    public Optional<String> optionalString(String key) {
        try {
            return Optional.of(bundle.getString(key));
        } catch (MissingResourceException ex) {
            return Optional.empty();
        }
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public Color getColor(String key) {
        return ColorFactory.createColor(getString(key));
    }

    public String formatMessage(String key, Object ... args) {
        return MessageFormat.format(bundle.getString(key), args);
    }
}