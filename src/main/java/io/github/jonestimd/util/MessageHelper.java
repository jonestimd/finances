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
package io.github.jonestimd.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import io.github.jonestimd.function.MessageConsumer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

@Deprecated
public class MessageHelper implements MessageConsumer {
    private final ResourceBundle bundle;
    private final String keyPrefix;
    private final Logger logger;

    public MessageHelper(ResourceBundle bundle, Class<?> contextClass) {
        this(bundle, contextClass.getPackage().getName() + '.', Logger.getLogger(contextClass));
    }

    public MessageHelper(ResourceBundle bundle, String keyPrefix, Logger logger) {
        this.bundle = bundle;
        this.keyPrefix = keyPrefix;
        this.logger = logger;
        logger.setResourceBundle(bundle);
    }

    public String getMessage(String code, Object ... args) {
        return MessageFormat.format(bundle.getString(getMessageKey(code)), args);
    }

    public String getMessageKey(String code) {
        return keyPrefix + code;
    }

    public String getLabel(String labelId) {
        return bundle.getString(getMessageKey(labelId));
    }

    public String getLabel(String componentId, String labelId) {
        return bundle.getString(keyPrefix + componentId + '.' + labelId);
    }

    public Logger getLogger() {
        return logger;
    }

    public void debug(String messageFormat, Object ...args) {
        if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format(messageFormat, args));
        }
    }

    public void infoLocalized(String messageKey, Object ...args) {
        logger.l7dlog(Level.INFO, getMessageKey(messageKey), args, null);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warnLocalized(String messageKey, Throwable throwable, Object ...args) {
        logger.l7dlog(Level.WARN, getMessageKey(messageKey), args, throwable);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void errorLocalized(String messageKey, Throwable throwable, Object ...args) {
        logger.l7dlog(Level.ERROR, getMessageKey(messageKey), args, throwable);
    }

    /**
     * Log an INFO message.
     * @param messageKey the resource bundle key for the message format
     * @param args the message arguments
     */
    @Override
    public void accept(String messageKey, Object... args) {
        infoLocalized(messageKey, args);
    }
}