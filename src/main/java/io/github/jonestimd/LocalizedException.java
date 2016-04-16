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
package io.github.jonestimd;

import java.text.MessageFormat;

public abstract class LocalizedException extends Exception {

    private Object[] messageArgs;

    protected LocalizedException(String messageKey, Object... messageArgs) {
        super(messageKey);
        this.messageArgs = messageArgs;
    }

    protected LocalizedException(String messageKey, Throwable cause, Object... messageArgs) {
        super(messageKey, cause);
        this.messageArgs = messageArgs;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }

    public String getMessageKey() {
        return super.getMessage();
    }

    public String getMessage() {
        return MessageFormat.format(getMessageFormat(super.getMessage()), messageArgs);
    }

    protected abstract String getMessageFormat(String messageKey);
}