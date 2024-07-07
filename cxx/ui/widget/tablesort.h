#ifndef TABLES_H
#define TABLES_H

#include "filterinput.h"
#include "../model/adaptertablemodel.h"
#include <QStatusBar>
#include <QTableView>

class TableSort : public QObject {
    Q_OBJECT
public:
    AdapterTableModel *model;
    QTableView table;
    QSortFilterProxyModel sortModel;
    FilterInput filterInput;
    QStatusBar *statusBar;
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

    void scrollTo(int rowIndex, int columnIndex);

    Q_SLOT void focusChanged(const QModelIndex &index);
};

#endif // TABLES_H
