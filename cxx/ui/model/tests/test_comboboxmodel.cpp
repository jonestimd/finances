#include <QTest>
#include "ui/finances.h"
#include "../comboboxmodel.h"

class TestComboBoxModel: public QObject {
    Q_OBJECT
private slots:
    void sortsOptions();
};

struct TestOption : NamedEntity {
    TestOption(domain_id id, QString name) {
        this->id = id;
        this->name = name;
    };
};

void TestComboBoxModel::sortsOptions() {
    TestOption o1(1, "aaa"), o2(2, "zzz"), o3(3, "bbb"), o4(4, "Bbb");
    const QList<const NamedEntity*> values{&o1, &o2, &o3, &o4};

    ComboBoxModel model(values, NamedEntity::getName);

    QStringList ids;
    QStringList names;
    for (auto i = 0; i < model.rowCount(QModelIndex{}); i++) {
        ids.append(model.index(i, 0).data(finances::EntityIdRole).toString());
        names.append(model.index(i, 0).data().toString());
    }
    QCOMPARE(ids.join(","), "1,4,3,2");
    QCOMPARE(names.join(","), "aaa,Bbb,bbb,zzz");
}

QTEST_MAIN(TestComboBoxModel)
#include "test_comboboxmodel.moc"
