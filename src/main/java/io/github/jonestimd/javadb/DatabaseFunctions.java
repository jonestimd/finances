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
package io.github.jonestimd.javadb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseFunctions {
    public static final String SELECT_SPLITS = "select ss.shares_out, ss.shares_in from stock_split ss where ss.security_id = ? and ss.date >= ?";

    public static BigDecimal adjustShares(Connection connection, Long securityId, Date fromDate, BigDecimal shares) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(SELECT_SPLITS);
        ps.setLong(1, securityId);
        ps.setDate(2, fromDate);
        if (ps.execute()) {
            ResultSet resultSet = ps.getResultSet();
            while (resultSet.next()) {
                shares = shares.multiply(resultSet.getBigDecimal("shares_out")).divide(resultSet.getBigDecimal("shares_in"));
            }
        }
        return shares;
    }
}