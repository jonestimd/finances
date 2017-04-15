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
package io.github.jonestimd.finance.file.quicken;

import java.util.ArrayList;
import java.util.List;

import io.github.jonestimd.LocalizedException;
import io.github.jonestimd.finance.swing.BundleType;

public class QuickenException extends LocalizedException {
    private static final String KEY_PREFIX = "import.qif.";

    public QuickenException(String messageKey, Object ... messageArgs) {
        super(KEY_PREFIX + messageKey, messageArgs);
    }

    public QuickenException(String messageKey, Throwable cause, Object ... messageArgs) {
        super(KEY_PREFIX + messageKey, cause, messageArgs);
    }

    protected String getMessageFormat(String messageKey) {
        return BundleType.MESSAGES.getString(messageKey);
    }

    public List<String> getMessages() {
        List<String> messages = new ArrayList<>();
        for (Throwable ex = this; ex instanceof QuickenException; ex = ex.getCause()) {
            messages.add(ex.getMessage());
            if (ex.getCause() != null && ! (ex.getCause() instanceof QuickenException) && ex.getCause().getMessage() != null) {
                messages.add(ex.getCause().getMessage());
            }
        }
        return messages;
    }

    public Throwable getRootCause() {
        Throwable root = this;
        while (root instanceof QuickenException) root = root.getCause();
        return root;
    }
}