#include <QTest>
#include <QDecNumber.hh>

class TestDecimal: public QObject {
    Q_OBJECT
private slots:
    void canConvert();
    void isValid();
};

void TestDecimal::canConvert() {
    QMetaType::registerConverter<QDecNumber, QString>(
        [](const QDecNumber &value) -> QString { return QString(value.toString()); }
    );
    // qRegisterMetaType(QMetaType::fromName("QDecNumber"));
    QDecNumber num = QDecNumber("123.456");
    QVariant variant = QVariant{};
    variant.setValue(num);
    QCOMPARE(variant.toString(), "123.456");
    QVERIFY(variant.canConvert<QString>());
}

void TestDecimal::isValid() {
    QDecNumber num = QDecNumber("123.456");
    QVariant variant = QVariant{};
    variant.setValue(num);
    QVERIFY(variant.isValid());
    QVERIFY(!variant.isNull());
}

QTEST_APPLESS_MAIN(TestDecimal)
#include "test_decimal.moc"
