// The MIT License (MIT)
//
// Copyright (c) 2021 Tim Jones
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
package io.github.jonestimd.finance.dao;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Consumer;

import com.google.common.base.Supplier;
import com.mchange.io.FileUtils;
import com.typesafe.config.Config;
import io.github.jonestimd.finance.config.ApplicationConfig;
import io.github.jonestimd.finance.dao.hibernate.AccountDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.AccountSummaryEventHandler;
import io.github.jonestimd.finance.dao.hibernate.CompanyDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.CompositeEventHandler;
import io.github.jonestimd.finance.dao.hibernate.CurrencyDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.DomainEventInterceptor;
import io.github.jonestimd.finance.dao.hibernate.DomainEventRecorder;
import io.github.jonestimd.finance.dao.hibernate.EventBuilder;
import io.github.jonestimd.finance.dao.hibernate.EventHandlerEventHolder;
import io.github.jonestimd.finance.dao.hibernate.NamedQueryInterceptor;
import io.github.jonestimd.finance.dao.hibernate.PayeeDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.SecurityDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.SecurityLotDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.SecuritySummaryEventHandler;
import io.github.jonestimd.finance.dao.hibernate.StockSplitDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.TransactionCategoryDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.TransactionDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.TransactionDetailDaoImpl;
import io.github.jonestimd.finance.dao.hibernate.TransactionGroupDaoImpl;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.hibernate.AuditInterceptor;
import io.github.jonestimd.hibernate.InterceptorChain;
import io.github.jonestimd.hibernate.MappedClassFilter;
import io.github.jonestimd.hibernate.TransactionInterceptor;
import io.github.jonestimd.reflect.PackageScanner;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import static io.github.jonestimd.finance.swing.FinanceApplication.*;

public class HibernateDaoContext implements DaoRepository {
    private static final String EVENT_SOURCE = "Services";
    private final Logger logger = Logger.getLogger(HibernateDaoContext.class);
    private final List<String> hibernateResources;
    private Configuration configuration;
    protected SessionFactory sessionFactory;
    private CompanyDao companyDao;
    private AccountDao accountDao;
    private PayeeDao payeeDao;
    private TransactionCategoryDao TransactionCategoryDao;
    private TransactionGroupDao transactionGroupDao;
    private TransactionDao transactionDao;
    private TransactionDetailDao transactionDetailDao;
    private CurrencyDao currencyDao;
    private SecurityDao securityDao;
    private StockSplitDao stockSplitDao;
    private SecurityLotDao securityLotDao;
    private ImportFileDao importFileDao;
    private Supplier<EventHandlerEventHolder> eventHandlerSupplier = () -> new CompositeEventHandler(
            new EventBuilder(EVENT_SOURCE),
            new AccountSummaryEventHandler(EVENT_SOURCE),
            new SecuritySummaryEventHandler(EVENT_SOURCE));
    private DomainEventInterceptor eventInterceptor = new DomainEventInterceptor(eventHandlerSupplier);

    public static HibernateDaoContext connect(boolean createSchema, DriverService driverService, Config config, Consumer<String> updateProgress)
            throws Exception {
        HibernateDaoContext daoContext = new HibernateDaoContext(driverService, config);
        if (createSchema) {
            updateProgress.accept(BundleType.LABELS.getString("database.status.creatingTables"));
            new SchemaBuilder(daoContext).createSchemaTables(driverService.getPostCreateSchemaScript()).seedReferenceData();
        }
        return daoContext;
    }

    public HibernateDaoContext(DriverService driverService, Config config) {
        this.hibernateResources = driverService.getHibernateResources();
        buildSessionFactory(config, driverService.getHibernateProperties());
        buildDaos();
    }

    private void buildSessionFactory(Config config, Properties hibernateProperties) {
        configuration = new Configuration(getMetadataSources());
        config.getConfig("finances.connection.properties").entrySet()
                .forEach(entry -> configuration.setProperty(entry.getKey(), entry.getValue().unwrapped().toString()));
        for (Entry<Object, Object> entry : hibernateProperties.entrySet()) {
            configuration.setProperty((String) entry.getKey(), (String) entry.getValue());
        }
        configuration.setInterceptor(new InterceptorChain(eventInterceptor, new AuditInterceptor()));
        sessionFactory = configuration.buildSessionFactory();
    }

    private MetadataSources getMetadataSources() {
        return getMetadataSources(new BootstrapServiceRegistryBuilder().build());
    }

    private MetadataSources getMetadataSources(ServiceRegistry serviceRegistry) {
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        new PackageScanner(new MappedClassFilter(), metadataSources::addAnnotatedClass, "io.github.jonestimd.finance.domain").visitClasses();
        metadataSources.addResource("io/github/jonestimd/finance/domain/queries.hbm.xml");
        hibernateResources.forEach(metadataSources::addResource);
        return metadataSources;
    }

    private void buildDaos() {
        companyDao = transactional(new CompanyDaoImpl(sessionFactory), CompanyDao.class);
        accountDao = transactional(new AccountDaoImpl(sessionFactory), AccountDao.class);
        payeeDao = transactional(new PayeeDaoImpl(sessionFactory), PayeeDao.class);
        TransactionCategoryDao = transactional(new TransactionCategoryDaoImpl(sessionFactory), TransactionCategoryDao.class);
        transactionGroupDao = transactional(new TransactionGroupDaoImpl(sessionFactory), TransactionGroupDao.class);
        transactionDao = transactional(new TransactionDaoImpl(sessionFactory), TransactionDao.class);
        transactionDetailDao = transactional(new TransactionDetailDaoImpl(sessionFactory), TransactionDetailDao.class);
        securityDao = transactional(new SecurityDaoImpl(sessionFactory), SecurityDao.class);
        currencyDao = transactional(new CurrencyDaoImpl(sessionFactory), CurrencyDao.class);
        stockSplitDao = transactional(new StockSplitDaoImpl(sessionFactory), StockSplitDao.class);
        securityLotDao = transactional(new SecurityLotDaoImpl(sessionFactory), SecurityLotDao.class);
        importFileDao = transactional(NamedQueryInterceptor.createDao(sessionFactory, ImportFileDao.class), ImportFileDao.class);
    }

    @Override
    public void generateSchema(List<String> postCreateScript) throws IOException {
        List<String> script = schemaCreationScript();
        try (Session session = sessionFactory.openSession()) {
            session.doWork(connection -> {
                executeSchemaScript(connection, script);
                if (!postCreateScript.isEmpty()) executeSchemaScript(connection, postCreateScript);
                connection.commit();
            });
        }
    }

    private List<String> schemaCreationScript() throws IOException {
        File tempFile = File.createTempFile("finances", ".ddl");
        tempFile.deleteOnExit();
        StandardServiceRegistry registry = configuration.getStandardServiceRegistryBuilder().build();
        MetadataSources metadataSources = getMetadataSources(registry);
        new SchemaExport()
                .setOutputFile(tempFile.getAbsolutePath())
                .createOnly(EnumSet.of(TargetType.SCRIPT), metadataSources.buildMetadata());
        String script = FileUtils.getContentsAsString(tempFile);
        List<String> lines = new ArrayList<>();
        String last = "";
        for (String line : script.split("\\n")) {
            if (line.startsWith("create table") || line.startsWith("alter table")) lines.add(line);
            else if (line.trim().startsWith("create function")) last = line;
            else {
                last += "\n" + line;
                if (line.trim().equals(";")) lines.add(last);
            }
        }
        return lines;
    }

    private void executeSchemaScript(Connection connection, List<String> sqlScript) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            for (String sqlStmt : sqlScript) {
                executeSchemaStatement(stmt, sqlStmt);
            }
        }
    }

    private void executeSchemaStatement(Statement stmt, String sql) {
        logger.debug("Executing schema statement: " + sql);
        try {
            stmt.executeUpdate(sql);
        }
        catch (SQLException ex) {
            logger.warn("Unsuccessful schema statement: " + sql, ex);
        }
    }

    @Override
    public <I, T extends I> I transactional(T target, Class<I> iface) {
        return iface.cast(Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[]{iface}, new TransactionInterceptor(target, sessionFactory)));
    }

    @Override
    public void doInTransaction(Runnable work) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                work.run();
                transaction.commit();
            } finally {
                if (transaction.getStatus() != TransactionStatus.COMMITTED) {
                    transaction.rollback();
                }
            }
        }
    }

    public CompanyDao getCompanyDao() {
        return companyDao;
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    public PayeeDao getPayeeDao() {
        return payeeDao;
    }

    public TransactionCategoryDao getTransactionCategoryDao() {
        return TransactionCategoryDao;
    }

    public TransactionGroupDao getTransactionGroupDao() {
        return transactionGroupDao;
    }

    public TransactionDao getTransactionDao() {
        return transactionDao;
    }

    @Override
    public TransactionDetailDao getTransactionDetailDao() {
        return transactionDetailDao;
    }

    public CurrencyDao getCurrencyDao() {
        return currencyDao;
    }

    public SecurityDao getSecurityDao() {
        return securityDao;
    }

    public StockSplitDao getStockSplitDao() {
        return stockSplitDao;
    }

    public SecurityLotDao getSecurityLotDao() {
        return securityLotDao;
    }

    @Override
    public ImportFileDao getImportFileDao() {
        return importFileDao;
    }

    public DomainEventRecorder getDomainEventRecorder() {
        return eventInterceptor;
    }

    public static void main(String[] args) {
        try {
            DriverService driverService = CONNECTION_CONFIG.loadDriver();
            HibernateDaoContext context = new HibernateDaoContext(driverService, ApplicationConfig.CONFIG);
            StandardServiceRegistry registry = context.configuration.getStandardServiceRegistryBuilder().build();
            MetadataSources metadataSources = context.getMetadataSources(registry);
            new SchemaExport().createOnly(EnumSet.of(TargetType.STDOUT), metadataSources.buildMetadata());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}