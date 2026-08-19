#include "dbtestcase.h"
#include "service/transactiondetailservice.h"
#include <QTest>

class TestTransactionDetailService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<TransactionDetailService*>("service");
        QTest::addColumn<domain_id>("accountId");
        QTest::addColumn<domain_id>("categoryId");
        QTest::addColumn<domain_id>("childCategoryId");
        QTest::addColumn<domain_id>("payeeId");
        QTest::addColumn<domain_id>("securityId");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &txDao = dbTestCase.transactionDao(driver);
            auto &detailDao = dbTestCase.detailDao(driver);
            auto &accountDao = dbTestCase.accountDao(driver);
            auto service = new TransactionDetailService{dbTestCase.connectionPool(driver), detailDao};
            auto companyId = dbTestCase.addCompany(driver, "Bank 1");
            auto accountId = dbTestCase.addAccount(driver, "Account 1", &AccountType::bank, companyId)->id.value();
            auto categoryId = dbTestCase.addCategory(driver, "parent");
            auto childCategoryId = dbTestCase.addCategory(driver, "child", categoryId);
            auto payeeId = dbTestCase.addPayee(driver, "Payee 1");
            auto securityId = dbTestCase.addSecurity(driver, "Security 1")->id.value();
            QTest::newRow(driver.toLocal8Bit()) << driver << service
                << accountId << categoryId << childCategoryId << payeeId << securityId;
        }
    }

    void findByText() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(TransactionDetailService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        auto transaction = factory::transaction(accountId);
        auto detail = factory::detail();
        detail->memo = "find me";
        dbTestCase.saveTransaction(driver, transaction, {detail, factory::detail()});

        auto result = service->findTransactionDetails({detail->memo, {}});

        QCOMPARE(result.size(), 1);
        QCOMPARE(result[0]->id.value(), detail->id.value());
        qDeleteAll(result);
    }

    void findByCategory() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(TransactionDetailService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, categoryId);
        QFETCH_GLOBAL(domain_id, childCategoryId);
        auto transaction = factory::transaction(accountId);
        auto detail1 = factory::detail("1.00", childCategoryId);
        auto detail2 = factory::detail("2.00", categoryId);
        dbTestCase.saveTransaction(driver, transaction, {detail1, detail2, factory::detail()});

        auto result = service->findTransactionDetails({"", {categoryId}});

        QCOMPARE(result.size(), 2);
        qDeleteAll(result);
    }

    void findByTextAndCategory() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(TransactionDetailService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, categoryId);
        QFETCH_GLOBAL(domain_id, childCategoryId);
        auto transaction = factory::transaction(accountId);
        auto detail1 = factory::detail("1.00", childCategoryId);
        auto detail2 = factory::detail("2.00", categoryId);
        detail1->memo = "find me";
        dbTestCase.saveTransaction(driver, transaction, {detail1, detail2, factory::detail()});

        auto result = service->findTransactionDetails({detail1->memo, {categoryId}});

        QCOMPARE(result.size(), 1);
        QCOMPARE(result[0]->id.value(), detail1->id.value());
        qDeleteAll(result);
    }
};

QTEST_GUILESS_MAIN(TestTransactionDetailService)
#include "test_transactiondetailservice.moc"
