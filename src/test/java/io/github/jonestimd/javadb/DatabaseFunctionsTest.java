package io.github.jonestimd.javadb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

import io.github.jonestimd.jdbc.DriverUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseFunctionsTest {
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;

    private final Date fromDate = new Date(0L);
    private final Long securityId = new Random().nextLong();

    @Test
    public void adjustSharesReturnsInputWhenNoSplits() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.execute()).thenReturn(false);

        BigDecimal adjustedShares = DatabaseFunctions.adjustShares(connection, securityId, fromDate, BigDecimal.TEN);

        assertThat(adjustedShares).isSameAs(BigDecimal.TEN);
        verify(connection).prepareStatement(DatabaseFunctions.SELECT_SPLITS);
        verify(statement).setLong(1, securityId);
        verify(statement).setDate(2, fromDate);
        verifyZeroInteractions(resultSet);
    }

    @Test
    public void adjustSharesMultipliesSharesBySplits() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.execute()).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getBigDecimal("shares_out")).thenReturn(new BigDecimal(2));
        when(resultSet.getBigDecimal("shares_in")).thenReturn(new BigDecimal(1));

        BigDecimal adjustedShares = DatabaseFunctions.adjustShares(connection, securityId, fromDate, BigDecimal.TEN);

        assertThat(adjustedShares).isEqualByComparingTo(new BigDecimal(20));
        verify(connection).prepareStatement(DatabaseFunctions.SELECT_SPLITS);
        verify(statement).setLong(1, securityId);
        verify(statement).setDate(2, fromDate);
    }

    @Test
    public void adjustSharesGetsConnectionForDerby() throws Exception {
        Driver driver = DriverUtils.mockDriver("jdbc:default:connection", connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.execute()).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getBigDecimal("shares_out")).thenReturn(new BigDecimal(2));
        when(resultSet.getBigDecimal("shares_in")).thenReturn(new BigDecimal(1));

        BigDecimal adjustedShares = DatabaseFunctions.adjustShares(securityId, fromDate, BigDecimal.TEN);

        assertThat(adjustedShares).isEqualByComparingTo(new BigDecimal(20));
        verify(connection).prepareStatement(DatabaseFunctions.SELECT_SPLITS);
        verify(statement).setLong(1, securityId);
        verify(statement).setDate(2, fromDate);
        DriverManager.deregisterDriver(driver);
    }
}