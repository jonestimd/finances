#include <QTest>
#include "QDecNumber.hh"
#include "dbtestcase.h"
#include "service/database/transactiondetaildao.h"
#include "service/transactionservice.h"

#define TX_UPDATES(...) QList<Transaction*>{__VA_ARGS__}
#define TX_ADDS(...) QList<Transaction*>{__VA_ARGS__}
#define TX_DELETES(...) QList<const Transaction*>{__VA_ARGS__}

#define DETAIL_UPDATES(...) QList<TransactionDetail*>{__VA_ARGS__}
#define DETAIL_ADDS(...) QMultiHash<const Transaction*, TransactionDetail*>{__VA_ARGS__}
#define DETAIL_DELETES(...) QList<const TransactionDetail*>{__VA_ARGS__}

#define DECIMAL_VARIANT(value) QVariant::fromValue(QDecNumber{value})

#define GET_DETAILS(txDetails) std::get<1>(txDetails)

template<class T>
QVariantList getIds(QList<T*> items) {
    QVariantList ids{};
    for (auto item : items) ids.append(item->id);
    return ids;
}

class TestTransactionService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QList<const Transaction*> transactions{};
    QList<const TransactionDetail*> details{};

    Transaction unsavedTransaction(QDate date = QDate::currentDate()) {
        QFETCH_GLOBAL(QVariant, accountId);
        return unsavedTransaction(accountId, date);
    }

    Transaction unsavedTransaction(QVariant accountId, QDate date = QDate::currentDate()) {
        QFETCH_GLOBAL(QVariant, payeeId);
        Transaction tx{accountId};
        tx.payeeId = payeeId;
        tx.date = date;
        return tx;
    }

    TransactionDetail unsavedDetail(const char *amount = "1.00") {
        TransactionDetail detail{};
        detail.amount = DECIMAL_VARIANT(amount);
        return detail;
    }

    typedef std::tuple<Transaction*, QList<TransactionDetail*>> TxDetails;

    TxDetails saveTransaction(QList<const char*> detailAmounts) {
        QFETCH_GLOBAL(QVariant, accountId);
        return saveTransaction(accountId, detailAmounts);
    }

    TxDetails saveTransaction(QVariant accountId, QList<const char*> detailAmounts) {
        QFETCH_GLOBAL(QString, driver);
        Transaction *tx = new Transaction(unsavedTransaction(accountId));
        Connection conn(dbTestCase.connectionPool(driver));
        transactionDao.add(conn.db, TX_ADDS(tx), TEST_USER);
        QList<TransactionDetail*> details{};
        for (auto &amount : detailAmounts) {
            TransactionDetail *detail = new TransactionDetail(unsavedDetail(amount));
            detail->transactionId = tx->id;
            details.append(detail);
        }
        transactionDetailDao.add(conn.db, details, TEST_USER);
        tx->detailIds = getIds(details);
        this->transactions.append(tx);
        for (auto detail : std::as_const(details)) this->details.append(detail);
        return TxDetails{tx, details};
    }

    QList<TxDetails> saveTransfer(QList<const char*> amounts) {
        const char *transferAmount = amounts.at(0);
        QString relatedAmount = transferAmount[0] == '-' ? QString(transferAmount[1]) : QString(transferAmount).prepend('-');
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(QVariant, altAccountId);
        auto tx = saveTransaction(amounts);
        auto relatedTx = saveTransaction(altAccountId, QList{relatedAmount.toLocal8Bit().constData()});
        auto detail = GET_DETAILS(tx).at(0);
        auto relatedDetail = GET_DETAILS(relatedTx).at(0);
        detail->relatedDetailId = relatedDetail->id;
        relatedDetail->relatedDetailId = detail->id;

        Connection conn(dbTestCase.connectionPool(driver));
        transactionDetailDao.setRelatedDetailIds(conn.db, {{relatedDetail, detail}});
        transactionDetailDao.setRelatedDetailIds(conn.db, {{detail, relatedDetail}});
        return QList{tx, relatedTx};
    }

    const Transaction *loadTransaction(const QVariant id) {
        QFETCH_GLOBAL(QString, driver);
        Connection conn(dbTestCase.connectionPool(driver));
        auto result = transactionDao.get(conn.db, QList{id});
        transactions.append(result.values());
        return result.value(id.toLongLong());
    }

    const TransactionDetail *loadDetail(const QVariant id) {
        QFETCH_GLOBAL(QString, driver);
        Connection conn(dbTestCase.connectionPool(driver));
        auto result = transactionDetailDao.get(conn.db, QList{id});
        details.append(result.values());
        return result.value(id.toLongLong());
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<TransactionService*>("service");
        QTest::addColumn<QVariant>("accountId");
        QTest::addColumn<QVariant>("altAccountId");
        QTest::addColumn<QVariant>("payeeId");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto service = new TransactionService{dbTestCase.connectionPool(driver)};
            auto companyId = dbTestCase.addCompany(driver, "Bank 1");
            auto accountId = dbTestCase.addAccount(driver, "Account 1", "BANK", companyId);
            auto altAccountId = dbTestCase.addAccount(driver, "Account 2", "BANK", companyId);
            auto payeeId = dbTestCase.addPayee(driver, "Payee 1");
            QTest::newRow(driver.toLocal8Bit()) << driver << service << accountId << altAccountId << payeeId;
        }
    }

    void update_addsNewTransaction() {
        QFETCH_GLOBAL(TransactionService*, service);
        Transaction tx = unsavedTransaction();
        TransactionDetail detail1 = unsavedDetail("1.00");
        TransactionDetail detail2 = unsavedDetail("2.00");
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(&tx), TX_DELETES(),
            DETAIL_UPDATES(), DETAIL_ADDS({&tx, &detail1}, {&tx, &detail2}), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        auto detailIds = getIds(changes.detailAdds.values());
        QCOMPARE(result.transactions.at(0)->detailIds, detailIds);
        QCOMPARE(result.details.size(), 2);
        QVERIFY(!tx.id.isNull());
        QVERIFY(!detail1.id.isNull());
        QVERIFY(!detail2.id.isNull());
        QCOMPARE(detail1.transactionId, tx.id);
        QCOMPARE(detail2.transactionId, tx.id);
    }

    void update_addsNewTransfer() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(QVariant, altAccountId);
        Transaction tx = unsavedTransaction();
        TransactionDetail detail = unsavedDetail("1.00");
        detail.transferAccountId = altAccountId;
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(&tx), TX_DELETES(),
            DETAIL_UPDATES(), DETAIL_ADDS({&tx, &detail}), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.details.size(), 2);
        QCOMPARE(result.details.at(0)->relatedDetailId, result.details.at(1)->id);
        QCOMPARE(result.details.at(1)->relatedDetailId, result.details.at(0)->id);
        QCOMPARE(result.transactions.size(), 2);
        QCOMPARE(result.transactions.at(1)->accountId, altAccountId);
        QCOMPARE(result.transactions.at(0)->id, result.details.at(0)->transactionId);
        QCOMPARE(result.transactions.at(1)->id, result.details.at(1)->transactionId);
        auto updatedDetail = loadDetail(detail.id);
        QCOMPARE(updatedDetail->relatedDetailId, result.details.at(1)->id);
        auto relatedDetail = loadDetail(result.details.at(1)->id);
        QCOMPARE(relatedDetail->relatedDetailId, detail.id);
    }

    void update_updatesTransactionAndDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        auto [tx, details] = saveTransaction(QList{"1.00", "2.00"});
        auto newDetail = unsavedDetail("3.45");
        tx->payeeId = QVariant{};
        tx->memo = "tx comment";
        auto detail0 = details.at(0);
        detail0->amount = DECIMAL_VARIANT("3.21");
        detail0->memo = "detail comment";
        TransactionUpdate changes{
            TX_UPDATES(tx), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(detail0), DETAIL_ADDS({tx, &newDetail}), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        auto detailIds = getIds(details);
        detailIds.append(newDetail.id);
        QCOMPARE(result.transactions.at(0)->detailIds, detailIds);
        auto updatedTx = loadTransaction(tx->id);
        QVERIFY(updatedTx->payeeId.isNull());
        QCOMPARE(updatedTx->memo, tx->memo);
        auto updatedDetail = loadDetail(detail0->id);
        QCOMPARE(updatedDetail->amount, detail0->amount);
        QCOMPARE(updatedDetail->memo, detail0->memo);
    }

    void update_updatesTransferAmount() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(TransactionService*, service);
        auto transfer = saveTransfer({"1.00", "2.00"});
        auto [tx, details] = transfer.at(0);
        details.at(0)->amount = DECIMAL_VARIANT("3.45");
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(details.at(0)), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.details.size(), 2);
        QCOMPARE(result.details.at(1)->id, details.at(0)->relatedDetailId);
        QCOMPARE(result.details.at(1)->amount.toString(), "-3.45");
    }

    void update_addsRelatedTransactionAndDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(QVariant, altAccountId);
        auto [tx, details] = saveTransaction(QList{"1.00", "2.34"});
        auto detail = details.at(1);
        detail->transferAccountId = altAccountId;
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(detail), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        QCOMPARE(result.transactions.at(0)->detailIds, QList{result.details.at(1)->id});
        QCOMPARE(result.details.size(), 2);
        auto relatedDetail = result.details.at(1);
        QCOMPARE(detail->relatedDetailId, relatedDetail->id);
        QCOMPARE(relatedDetail->relatedDetailId, detail->id);
        QCOMPARE(relatedDetail->amount.toString(), "-2.34");
    }

    void update_removesRelatedTransactionAndDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(QVariant, altAccountId);
        auto transfer = saveTransfer({"1.23"});
        auto [tx, details] = transfer.at(0);
        auto [relatedTx, relatedDetails] = transfer.at(1);
        details.at(0)->transferAccountId = QVariant{};
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(details), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(loadTransaction(relatedTx->id), nullptr);
        QCOMPARE(loadDetail(relatedDetails.at(0)->id), nullptr);
    }

    void update_deletesTransactionAndDetails() {
        QFETCH_GLOBAL(TransactionService*, service);
        auto [tx1, details1] = saveTransaction(QList{"1.00", "2.00"});
        auto [tx2, details2] = saveTransaction(QList{"3.00"});
        auto transfer1 = saveTransfer({"4.00"}); // delete transfer tx
        auto [tx3, details3] = transfer1.at(0);
        auto [tx4, details4] = transfer1.at(1);
        auto transfer2 = saveTransfer({"5.00", "6.00"}); // delete transfer detail
        auto [tx5, details5] = transfer2.at(0);
        auto [tx6, details6] = transfer2.at(1);
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(tx2, tx4),
            DETAIL_UPDATES(), DETAIL_ADDS(), DETAIL_DELETES(details1.at(0), details5.at(0)),
        };

        service->update(changes, TEST_USER);

        QCOMPARE(loadTransaction(tx2->id), nullptr); // deleted transaction
        QCOMPARE(loadDetail(details2.at(0)->id), nullptr);
        QCOMPARE(loadTransaction(tx3->id), nullptr); // deleted transfer
        QCOMPARE(loadTransaction(tx4->id), nullptr);
        QCOMPARE(loadDetail(details3.at(0)->id), nullptr);
        QCOMPARE(loadDetail(details4.at(0)->id), nullptr);
        QCOMPARE(loadDetail(details1.at(0)->id), nullptr); // deleted detail
        QCOMPARE(loadDetail(details5.at(0)->id), nullptr); // deleted transfer detail
        QVERIFY(loadDetail(details5.at(1)->id) != nullptr);
        QCOMPARE(loadDetail(details6.at(0)->id), nullptr);
        QVERIFY(loadTransaction(tx5->id) != nullptr);
        QCOMPARE(loadTransaction(tx6->id), nullptr);
    }

    void cleanup() {
        for (auto tx : std::as_const(transactions)) delete tx;
        transactions.clear();
        for (auto detail : std::as_const(details)) delete detail;
        details.clear();
    }

    void cleanupTestCase() {
        // TODO shutdown connection pool?
    }
};

QTEST_GUILESS_MAIN(TestTransactionService)
#include "test_transactionservice.moc"
