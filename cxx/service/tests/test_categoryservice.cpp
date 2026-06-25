#include <QTest>
#include "dbtestcase.h"
#include "service/categoryservice.h"

class TestCategoryService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QList<const Category*> categories{};

    Category *addCategory(const QString &driver, const QString &name, Category *parent = nullptr) {
        Connection conn(dbTestCase.connectionPool(driver));
        auto testName = QTest::currentTestFunction();
        Category *category = new Category;
        category->name = QString("%0:%1").arg(testName, name);
        if (parent) category->parentId.emplace(parent->id.value());
        dbTestCase.categoryDao(driver).add(conn.db, {category}, TEST_USER);
        if (parent) parent->childIds.append(category->id.value());
        this->categories.append(category);
        return category;
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<CategoryService*>("service");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &dao = dbTestCase.categoryDao(driver);
            auto &detailDao = dbTestCase.detailDao(driver);
            auto service = new CategoryService{dbTestCase.connectionPool(driver), dao, detailDao};
            QTest::newRow(driver.toLocal8Bit()) << driver << service;
        }
    }

    void setParent_returnsOldParent() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = addCategory(driver, "parent");
        auto child = addCategory(driver, "child", parent);

        auto result = service->setParent(child, {}, TEST_USER);

        QCOMPARE(result.size(), 2);
        QCOMPARE(result.value(parent->id.value())->name, parent->name);
        QCOMPARE(result.value(parent->id.value())->childIds, {});
        QCOMPARE(result.value(child->id.value())->parentId, {});
    }

    void setParent_returnsOldAndNewParents() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = addCategory(driver, "parent");
        auto child = addCategory(driver, "child", parent);
        auto newParent = addCategory(driver, "new parent");

        auto result = service->setParent(child, newParent->id.value(), TEST_USER);

        QCOMPARE(result.size(), 3);
        QCOMPARE(result.value(parent->id.value())->name, parent->name);
        QCOMPARE(result.value(parent->id.value())->childIds, {});
        QCOMPARE(result.value(newParent->id.value())->name, newParent->name);
        QCOMPARE(result.value(newParent->id.value())->childIds, QList<qlonglong>{child->id.value()});
        QCOMPARE(result.value(child->id.value())->parentId.value(), newParent->id.value());
    }

    void merge_updatesChildren() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = addCategory(driver, "parent");
        auto child = addCategory(driver, "child", parent);
        auto toMerge = addCategory(driver, "other parent");
        auto otherChild = addCategory(driver, "other child", toMerge);

        auto result = service->merge(toMerge, parent->id.value(), TEST_USER);

        QCOMPARE(result.size(), 2);
        QCOMPARE(result.value(parent->id.value())->name, parent->name);
        QCOMPARE(result.value(otherChild->id.value())->name, otherChild->name);
        QCOMPARE(result.value(otherChild->id.value())->parentId, parent->id);
        QList<qlonglong> childIds{child->id.value(), otherChild->id.value()};
        QCOMPARE(result.value(parent->id.value())->childIds, childIds);
    }

    void cleanup() {
        for (auto category : std::as_const(categories)) delete category;
        categories.clear();
    }
};

QTEST_GUILESS_MAIN(TestCategoryService)
#include "test_categoryservice.moc"
