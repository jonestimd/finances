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
package io.github.jonestimd.finance.file.quicken.qif;

public class CategoryParser {

    private boolean transfer;
    private String accountName;
    private String[] categoryNames;
    private String groupName;

    public CategoryParser(String qifCategory) {
        if (qifCategory != null && qifCategory.length() > 0) {
            int index = qifCategory.indexOf('/');
            if (index >= 0) {
                groupName = qifCategory.substring(index + 1);
                qifCategory = qifCategory.substring(0, index);
                if (groupName.length() == 0) {
                    groupName = null;
                }
            }
            if (qifCategory.length() > 0) {
                transfer = qifCategory.charAt(0) == '[' && qifCategory.indexOf(']') == qifCategory.length()-1;
                if (transfer) {
                    accountName = qifCategory.substring(1, qifCategory.length()-1);
                }
                else {
                    categoryNames = qifCategory.split(":");
                }
            }
        }
    }

    public String[] getCategoryNames() {
        return categoryNames;
    }

    public boolean hasCatetories() {
        return categoryNames != null && categoryNames.length > 0;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean hasGroup() {
        return groupName != null;
    }

    public boolean isTransfer() {
        return transfer;
    }
}
