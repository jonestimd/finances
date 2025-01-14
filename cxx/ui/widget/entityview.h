#ifndef ENTITY_VIEW_H
#define ENTITY_VIEW_H

#include "filterinput.h"
#include "statusbar.h"
#include "tableitemdelegate.h"
#include "../model/adapteritemmodel.h"
#include <QStatusBar>
#include <QTableView>
#include <QTreeView>

// TODO - break up?
//   - save/restore
//   - actions (toolbar?)
class EntityView : public QObject {
    Q_OBJECT
    QWidget *const window;
    TableItemDelegate itemDelegate;
protected:
    StatusBar *statusBar;

    virtual QHeaderView *viewHeader() const = 0;

public:
    AdapterItemModel *const model; // TODO move to window save/restore class?
    QAbstractItemView *const itemView;
    QSortFilterProxyModel sortModel;
    FilterInput *const filterInput;
    QToolBar toolbar;
    QAction *const saveAction;
    const QString defaultSort;

    EntityView(QWidget *window, AdapterItemModel *model, QAbstractItemView *itemView, StatusBar *statusBar,
               const QString filterLabel, const QString defaultSort,
               const char *saveSlot, const char *loadSlot, QList<QAction*> actions);

    int columnIndex(const QString name) const;

    QModelIndex selectedIndex();

    void enableColumnResize();

    void setColumnResize(const std::vector<int> stretchColumns);

    bool focusFilter(QKeyEvent *event);

    void saveSort(QSettings *settings);

    void saveSizes(QString group, QSettings *settings);

    void restore(QString group, QSettings *settings);

    void startEdit(int rowIndex);

    void setEnabled(auto action, std::function<bool(int)> enableDelete);

    void loadData(QString statusMessage, std::function<void()> doLoad);
    void saveData(QString statusMessage, std::function<void()> doSave);

    void enableUi();

private:
    QAction *addAction(const QString text);
    QAction *deleteAction(const QString text, std::function<bool(const QModelIndex &)> enableDelete = nullptr);
    QAction *undoAction();

public Q_SLOTS:
    void dataChanged();
    void showValidation(const QModelIndex &index);
    void addRow();
    void queueDeletes();
    void undoChanges();
};

class EntityTable : public EntityView {
public:
    EntityTable(QWidget *window, AdapterItemModel *model, StatusBar *statusBar, const QString filterLabel, const QString defaultSort,
                const char *saveSlot, const char *loadSlot, QList<QAction*> actions = QList<QAction*>{});

    ~EntityTable();

protected:
    inline QTableView *tableView() const;
    virtual QHeaderView *viewHeader() const override;
};

class EntityTree : public EntityView {
public:
    EntityTree(QWidget *window, AdapterItemModel *model, StatusBar *statusBar, const QString filterLabel, const QString defaultSort,
                const char *saveSlot, const char *loadSlot, QList<QAction*> actions = QList<QAction*>{});

    ~EntityTree();

protected:
    inline QTreeView *treeView() const;
    virtual QHeaderView *viewHeader() const override;
};

#endif // ENTITY_VIEW_H
