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
package io.github.jonestimd.commandline;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommandLine {

    private static final String LAST_OPTION = "--";

    private Map<String, String> options = new HashMap<String, String>();
    private List<String> inputs = Collections.emptyList();

    public CommandLine(String[] args) {
        int index = parseOptions(args);
        inputs = Arrays.asList(Arrays.copyOfRange(args, index, args.length));
    }

    private int parseOptions(String[] args) {
        int index = 0;
        while (args.length > index && isOption(args[index])) {
            if (parseOption(args[index++])) break;
        }
        return index;
    }

    private boolean isOption(String arg) {
        return arg.charAt(0) == '-';
    }

    private boolean parseOption(String option) {
        String value = "";
        int equals = option.indexOf('=');
        if (equals > 0) {
            value = option.substring(equals+1);
            option = option.substring(0, equals);
        }
        options.put(option, value);
        return option.equals(LAST_OPTION);
    }

    public boolean hasOption(String name) {
        return options.containsKey(name);
    }

    public String getOption(String name) {
        return options.get(name);
    }

    public int getInputCount() {
        return inputs.size();
    }

    public String getInput(int index) {
        return inputs.get(index);
    }
}
