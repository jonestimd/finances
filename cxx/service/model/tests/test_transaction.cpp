#include <QTest>
#include "service/model/transaction.h"

class TestTransaction : public QObject {
    Q_OBJECT

    Transaction* newTransaction(std::optional<domain_id> id, int dateOffset = 0) {
        auto tx = new Transaction;
        tx->id = id;
        tx->date = QDate::currentDate().addDays(dateOffset);
        return tx;
    }

    void addRows(const char* lessText, Transaction* less, const char* rightText, Transaction* right) {
        QTest::addRow("%s < %s", lessText, rightText) << less << right << true;
        QTest::addRow("%s !< %s", rightText, lessText) << right << less << false;
    }

private slots:
    void lessThan_data() {
        QTest::addColumn<Transaction*>("left");
        QTest::addColumn<Transaction*>("right");
        QTest::addColumn<bool>("result");
        auto emptyTx = new Transaction;

        QTest::addRow("empty !< empty") << emptyTx << emptyTx << false;
        addRows("with date", newTransaction({}), "empty", emptyTx);
        addRows("with id", newTransaction(1), "empty", emptyTx);
        addRows("early date, high ID", newTransaction(2, -1), "late date, low ID", newTransaction(1));
        addRows("low ID", newTransaction(1), "high ID", newTransaction(2));
    }

    void lessThan() {
        QFETCH(Transaction*, left);
        QFETCH(Transaction*, right);
        QFETCH(bool, result);

        QCOMPARE(*left < *right, result);
    }
};

QTEST_GUILESS_MAIN(TestTransaction)
#include "test_transaction.moc"
