#ifndef POD_TABLE_MODEL_H
#define POD_TABLE_MODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "poditemmodel.h"
#include "columnadapter.h"

template<Copyable Row, class Store>
class PodTableModel : public PodItemModel<Row, Store> {
public:
    explicit PodTableModel(const Store *store, const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : PodItemModel<Row, Store>(store, columns, parent)
    {}

    // QAbstractItemModel interface
    QModelIndex index(int row, int column, const QModelIndex &parent = QModelIndex()) const override {
        return this->hasIndex(row, column, parent) ? this->createIndex(row, column) : QModelIndex();
    }
\
    QModelIndex parent(const QModelIndex &child) const override {
        return QModelIndex();
    }

    int rowCount(const QModelIndex &parent = QModelIndex()) const override {
        if (parent.isValid()) return 0;
        return this->rootIds.length() + this->pendingAdds().length();
    };
};

#endif // POD_TABLE_MODEL_H
