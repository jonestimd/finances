#include "ui/widget/treeview.h"
#include <QRangeModel>
#include <QTest>
#include <qheaderview.h>

class TreeRow;
struct TreeTraversal;

using Tree = std::vector<TreeRow*>;
class TreeRow {
    Q_GADGET
    Q_PROPERTY(QString m_name MEMBER m_name);
    Q_PROPERTY(QString m_description MEMBER m_description);
    Q_PROPERTY(QString readonly READ readonly);
    Q_PROPERTY(QString m_other MEMBER m_other);

    QString m_name;
    QString m_description;
    QString m_other;

    TreeRow *m_parent = nullptr;
    std::optional<Tree> m_children;

    friend struct TreeTraversal;

public:
    QVariant id;

    TreeRow() = default;
    TreeRow(QString name, QString description, QString other = "", std::optional<Tree> children = {})
        : m_name{name}, m_description{description}, m_other{other}, m_children{children} {}
    // move-only
    TreeRow(TreeRow &&) = default;
    TreeRow &operator=(TreeRow &&) = default;

    ~TreeRow() {
        if (m_children) qDeleteAll(*m_children);
    }

    QString readonly() {
        return "can't change me";
    }
};

struct TreeTraversal {
    TreeRow *newRow() const { return new TreeRow; }
    void deleteRow(TreeRow *row) { delete row; }

    const TreeRow *parentRow(const TreeRow &row) const { return row.m_parent; }
    void setParentRow(TreeRow &row, TreeRow *parent) { row.m_parent = parent; }
    const std::optional<Tree> &childRows(const TreeRow &row) const { return row.m_children; }
    std::optional<Tree> &childRows(TreeRow &row) { return row.m_children; }
};

class TestTreeView : public QObject {
    Q_OBJECT
    TreeView view;
    QRangeModel *model;

    QModelIndex index(int row, int column, int parent = -1, int grandParent = -1) {
        auto parentIndex = grandParent < 0 ? model->index(parent, 0) : model->index(parent, 0, model->index(grandParent, 0));
        return model->index(row, column, parentIndex);
    }

private slots:
    void initTestCase() {
        Tree data{
            new TreeRow{"p1", "Parent 1", "xxx", {{
                new TreeRow{"p1c1", "p1 Child 1"},
                new TreeRow{"p1c2", "p1 Child 2", "", {{new TreeRow{"p1c2g1", "p1 Grandchild 1"}}}},
            }}},
            new TreeRow{"p2", "Parent 2", "", {{new TreeRow{"p2c1", "p2 Child 1"}}}},
            new TreeRow{"p3", "Parent 3", "", {{new TreeRow{"p3c1", "p3 Child 1"}}}},
        };
        model = new QRangeModel(std::move(data), TreeTraversal{});
        view.setModel(model);
    }

    void move_data() {
        QTest::addColumn<Qt::Key>("key");
        QTest::addColumn<bool>("hideColumn");
        QTest::addColumn<bool>("expanded");
        QTest::addColumn<QModelIndex>("start");
        QTest::addColumn<QModelIndex>("end");
        QTest::addColumn<bool>("endExpanded");

        QTest::addRow("->: column 1 row expanded") << Qt::Key_Right << false << true << index(0, 0) << index(0, 1) << true;
        QTest::addRow("->: column 2 row expanded") << Qt::Key_Right << false << true << index(0, 1) << index(0, 2) << true;
        QTest::addRow("->: column 3 row expanded") << Qt::Key_Right << false << true << index(0, 2) << index(0, 3) << true;
        QTest::addRow("->: column 4 row expanded") << Qt::Key_Right << false << true << index(0, 3) << index(0, 0) << true;
        QTest::addRow("->: column 1 row collapsed") << Qt::Key_Right << false << false << index(0, 0) << index(0, 0) << true;
        QTest::addRow("->: column 2 row collapsed") << Qt::Key_Right << false << false << index(0, 1) << index(0, 2) << false;
        QTest::addRow("->: column 3 row collapsed") << Qt::Key_Right << false << false << index(0, 2) << index(0, 3) << false;
        QTest::addRow("->: column 4 row collapsed") << Qt::Key_Right << false << false << index(0, 3) << index(0, 0) << false;

        QTest::addRow("<-: column 1 row expanded") << Qt::Key_Left << false << true << index(0, 0) << index(0, 0) << false;
        QTest::addRow("<-: column 2 row expanded") << Qt::Key_Left << false << true << index(0, 1) << index(0, 0) << true;
        QTest::addRow("<-: column 3 row expanded") << Qt::Key_Left << false << true << index(0, 2) << index(0, 1) << true;
        QTest::addRow("<-: column 4 row expanded") << Qt::Key_Left << false << true << index(0, 3) << index(0, 2) << true;
        QTest::addRow("<-: column 1 row collapsed") << Qt::Key_Left << false << false << index(0, 0) << index(0, 3) << false;
        QTest::addRow("<-: column 2 row collapsed") << Qt::Key_Left << false << false << index(0, 1) << index(0, 0) << false;
        QTest::addRow("<-: column 3 row collapsed") << Qt::Key_Left << false << false << index(0, 2) << index(0, 1) << false;
        QTest::addRow("<-: column 4 row collapsed") << Qt::Key_Left << false << false << index(0, 3) << index(0, 2) << false;

        QTest::addRow("->: 2nd column hidden") << Qt::Key_Right << true << true << index(0, 0) << index(0, 2) << true;
        QTest::addRow("<-: 2nd column hidden") << Qt::Key_Left << true << true << index(0, 2) << index(0, 0) << true;

        QTest::addRow("tab to child") << Qt::Key_Tab << false << true << index(0, 3) << index(0, 0, 0) << true;
        QTest::addRow("tab to grandchild") << Qt::Key_Tab << false << true << index(1, 3, 0) << index(0, 0, 1, 0) << true;
        QTest::addRow("tab to sibling") << Qt::Key_Tab << false << true << index(0, 3, 0) << index(1, 0, 0) << true;
        QTest::addRow("tab to next parent") << Qt::Key_Tab << false << true << index(0, 3, 1) << index(2, 0) << true;
        QTest::addRow("tab skips readonly") << Qt::Key_Tab << false << true << index(0, 1) << index(0, 3) << true;

        QTest::addRow("back-tab to previous child") << Qt::Key_Backtab << false << true << index(2, 0) << index(0, 3, 1) << true;
        QTest::addRow("back-tab to grandchild") << Qt::Key_Backtab << false << true << index(1, 0) << index(0, 3, 1, 0) << true;
        QTest::addRow("back-tab to sibling") << Qt::Key_Backtab << false << true << index(1, 0, 0) << index(0, 3, 0) << true;
        QTest::addRow("back-tab to parent") << Qt::Key_Backtab << false << true << index(0, 0, 0) << index(0, 3) << true;
        QTest::addRow("back-tab skips readlony") << Qt::Key_Backtab << false << true << index(0, 3) << index(0, 1) << true;
    }

    void move() {
        QFETCH(Qt::Key, key);
        QFETCH(bool, hideColumn);
        QFETCH(bool, expanded);
        QFETCH(bool, endExpanded);
        QFETCH(QModelIndex, start);
        QFETCH(QModelIndex, end);
        if (hideColumn) view.header()->hideSection(1);
        view.setCurrentIndex(start);
        if (expanded) view.expand(start.siblingAtColumn(0));

        QTest::keyClick(&view, key);

        QCOMPARE(view.currentIndex(), end);
        QCOMPARE(view.isExpanded(start.siblingAtColumn(0)), endExpanded);
    }

    void cleanup() {
        view.header()->showSection(1);
        view.collapseAll();
    }
};

QTEST_MAIN(TestTreeView)
#include "test_treeview.moc"