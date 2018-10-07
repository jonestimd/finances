// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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
package io.github.jonestimd.finance.swing.event;

import javax.swing.JComponent;

import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.swing.window.FrameAction;
import io.github.jonestimd.swing.window.FrameManager;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SingletonFrameActions {
    public static FrameAction<WindowType> forType(Object source, WindowType type, String resourcePrefix, FrameManager<WindowType> frameManager) {
        return new FrameAction<>(LABELS.get(), resourcePrefix, frameManager, type);
    }

    public static FrameAction<WindowType> forAccounts(JComponent source, FrameManager<WindowType> frameManager) {
        return forType(source, WindowType.ACCOUNTS, "action.viewAccounts", frameManager);
    }
}