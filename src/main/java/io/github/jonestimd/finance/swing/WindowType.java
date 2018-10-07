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
package io.github.jonestimd.finance.swing;

import io.github.jonestimd.swing.window.WindowInfo;

public enum WindowType implements WindowInfo {
    ACCOUNTS(true, "accounts"),
    CATEGORIES(true, "categories"),
    PAYEES(true, "payees"),
    SECURITIES(true, "securities"),
    ACCOUNT_SECURITIES(true, "accountSecurities"),
    TRANSACTION_GROUPS(true, "transactionGroups"),
    TRANSACTIONS(false, "transactions"),
    TRANSACTION_DETAILS(false, "transactionDetails");

    private final boolean singleton;
    private final String resourcePrefix;

    WindowType(boolean singleton, String frameId) {
        this.singleton = singleton;
        this.resourcePrefix = "window." + frameId;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public String getResourcePrefix() {
        return resourcePrefix;
    }
}