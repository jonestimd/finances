// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.function;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public interface MessageConsumer {
    /**
     * Output a message.
     * @param pattern the resource bundle key for the message format
     * @param args the message arguments
     */
    void accept(String pattern, Object... args);

    /**
     * Create a {@code MessageConsumer} that formats messages and passes them to a {@link Consumer}.
     * @param bundle source of the message formats
     * @param consumer receiver of the formatted messages
     */
    static MessageConsumer forBundle(ResourceBundle bundle, Consumer<String> consumer) {
        return (key, args) -> consumer.accept(MessageFormat.format(bundle.getString(key), args));
    }
}
