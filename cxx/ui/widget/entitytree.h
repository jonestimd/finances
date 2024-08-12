#ifndef ENTITY_TREE_H
#define ENTITY_TREE_H

#include "filterinput.h"
#include "statusbar.h"
#include "tableitemdelegate.h"
#include "../model/adapteritemmodel.h"
#include <QStatusBar>
#include <QTreeView>

class EntityTree : public QObject {
    Q_OBJECT
    QWidget *const window;
    TableItemDelegate itemDelegate;
public:
    AdapterItemModel *model;
    QTreeView table;
    QSortFilterProxyModel sortModel;
    FilterInput *filterInput;
    QToolBar toolbar;
    StatusBar statusBar;
    QAction *const saveAction;
    const QString defaultSort;

    EntityTree(QWidget *window, AdapterItemModel *model, const QString filterLabel, const QString defaultSort, const char *saveSlot, const char *loadSlot,
                QList<QAction*> actions = QList<QAction*>{});

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

#endif // ENTITY_TREE_H
