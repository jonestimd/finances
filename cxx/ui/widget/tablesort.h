#ifndef TABLES_H
#define TABLES_H

#include "filterinput.h"
#include "../model/adaptertablemodel.h"
#include <QStatusBar>
#include <QTableView>

struct TableSort {
    AdapterTableModel *model;
    QTableView table;
    QSortFilterProxyModel sortModel;
    FilterInput filterInput;
    QString defaultSort;

    TableSort(QWidget *parent, AdapterTableModel *model, const char *filterLabel,
              const char *defaultSort = nullptr, QStatusBar *statusBar = nullptr);

    int columnIndex(const QString name) const;

    void enableColumnResize();

    void setColumnResize(const std::vector<int> stretchColumns);

    bool focusFilter(QKeyEvent *event);

    void saveSort(QSettings *settings);

    void saveSizes(QString group, QSettings *settings);

    void restore(QString group, QSettings *settings);
};

#endif // TABLES_H
