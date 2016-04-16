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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class QuickenRecord {
    private static final Pattern DATE_PATTERN = Pattern.compile("([0-9]{1,2})/ ?([0-9]{1,2})(['/]) ?([0-9]{1,2})");
    public static final char END = '^';

    private final long startingLine;
    private ListMultimap<Character, String> codeValues = ArrayListMultimap.create();

    public QuickenRecord(long startingLine) {
        this.startingLine = startingLine;
    }

    public long getStartingLine() {
        return startingLine;
    }

    private Date parseDate(String date) throws QuickenException {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        Matcher matcher = DATE_PATTERN.matcher(date);
        if (! matcher.matches()) {
            throw new QuickenException("invalidDate", date, startingLine);
        }
        calendar.set(Calendar.MONTH, Integer.parseInt(matcher.group(1))-1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(2)));

        int year = Integer.parseInt(matcher.group(4));
        year += "'".equals(matcher.group(3)) ? 2000 : 1900;
        calendar.set(Calendar.YEAR, year);

        return calendar.getTime();
    }

    public Date getDate(char code) throws QuickenException {
        return getDate(code, 0);
    }

    public Date getDate(char code, int index) throws QuickenException {
        return codeValues.containsKey(code) ? parseDate(codeValues.get(code).get(index)) : null;
    }

    public BigDecimal getBigDecimal(char code) {
        return getBigDecimal(code, 0);
    }

    public BigDecimal getBigDecimal(char code, int index) {
        return codeValues.containsKey(code) ? new BigDecimal(codeValues.get(code).get(index).replaceAll(",", "")) : BigDecimal.ZERO;
    }

    public String getValue(char code) {
        return codeValues.containsKey(code) ? codeValues.get(code).get(0) : null;
    }

    public List<BigDecimal> getBigDecimals(char code) {
        List<String> stringValues = codeValues.get(code);
        List<BigDecimal> result = new ArrayList<BigDecimal>(stringValues.size());
        for (String value : stringValues) {
            result.add(new BigDecimal(value.replaceAll(",", "")));
        }
        return result;
    }

    public List<String> getValues(char code) {
        return codeValues.containsKey(code) ? codeValues.get(code) : Collections.<String>emptyList();
    }

    public boolean hasValue(char code) {
        return codeValues.containsKey(code);
    }

    public void setValue(char code, String value) {
        codeValues.put(code, value);
    }

    public boolean isEmpty() {
        return codeValues.isEmpty() || codeValues.size() == 1 && codeValues.containsKey(END);
    }

    public boolean isComplete() {
        return hasValue(END);
    }

    public boolean isValid() {
        return codeValues.isEmpty() || codeValues.containsKey(END);
    }

    protected int size() {
        return codeValues.size();
    }

    public String toString() {
        return codeValues.toString();
    }
}