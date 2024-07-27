#ifndef TABLES_H
#define TABLES_H

#include "filterinput.h"
#include "tableitemdelegate.h"
#include "../model/adaptertablemodel.h"
#include <QStatusBar>
#include <QTableView>

class TableSort : public QObject {
    Q_OBJECT
    TableItemDelegate itemDelegate;
public:
    AdapterTableModel *model;
    QTableView table;
    QSortFilterProxyModel sortModel;
    FilterInput filterInput;
    QStatusBar *statusBar;
    const QString defaultSort;

    TableSort(QWidget *parent, AdapterTableModel *model, const QString filterLabel,
              const QString defaultSort, QStatusBar *statusBar = nullptr);

    int columnIndex(const QString name) const;

    QModelIndex selectedIndex();

    void enableColumnResize();

    void setColumnResize(const std::vector<int> stretchColumns);

    bool focusFilter(QKeyEvent *event);

    void saveSort(QSettings *settings);

    void saveSizes(QString group, QSettings *settings);

    void restore(QString group, QSettings *settings);

    void startEdit(int rowIndex);

    QAction *addAction(const QString text);
    QAction *deleteAction(const QString text, std::function<bool(int)> enableDelete = nullptr);
    QAction *undoAction();

    void setEnabled(auto action, std::function<bool(int)> enableDelete);

public Q_SLOTS:
    void showValidation(const QModelIndex &index);
    void addRow();
    void queueDeletes();
    void undoChanges();
};

#endif // TABLES_H
