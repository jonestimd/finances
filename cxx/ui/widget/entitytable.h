#ifndef ENTITY_TABLE_H
#define ENTITY_TABLE_H

#include "filterinput.h"
#include "statusbar.h"
#include "tableitemdelegate.h"
#include "../model/adaptertablemodel.h"
#include <QStatusBar>
#include <QTableView>

class EntityTable : public QObject {
    Q_OBJECT
    QWidget *const window;
    TableItemDelegate itemDelegate;
public:
    AdapterTableModel *model;
    QTableView table;
    QSortFilterProxyModel sortModel;
    FilterInput *filterInput;
    QToolBar toolbar;
    StatusBar statusBar;
    QAction *const saveAction;
    const QString defaultSort;

    EntityTable(QWidget *window, AdapterTableModel *model, const QString filterLabel, const QString defaultSort, const char *saveSlot, const char *loadSlot,
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

private:
    QAction *addAction(const QString text);
    QAction *deleteAction(const QString text, std::function<bool(int)> enableDelete = nullptr);
    QAction *undoAction();

public Q_SLOTS:
    void dataChanged();
    void showValidation(const QModelIndex &index);
    void addRow();
    void queueDeletes();
    void undoChanges();
};

#endif // ENTITY_TABLE_H
