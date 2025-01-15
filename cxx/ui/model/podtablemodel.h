#ifndef POD_TABLE_MODEL_H
#define POD_TABLE_MODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "entitystore.h"
#include "poditemmodel.h"
#include "columnadapter.h"

template<Copyable Row, class Service>
class PodTableModel : public PodItemModel<Row>
{
protected:
    QList<qlonglong> rowIds;
    const EntityStore<Row, Service> *const store;

    int childCount(const QModelIndex &index) const override {
        return index.isValid() ? 0 : rowIds.length();
    }

public:
    explicit PodTableModel(const EntityStore<Row, Service> *store, const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : PodItemModel<Row>(columns, parent)
        , store{store}
        , rowIds(store->ids())
    {}

    void setRows(const QList<qlonglong> rowIds) {
        this->clearChanges();
        this->beginResetModel();
        this->rowIds.clear();
        this->rowIds.append(rowIds);
        this->endResetModel();
    }

    const Row *getRow(const QModelIndex &index) const override {
        auto i = rowIds.length();
        if (index.row() < i) {
            auto id = rowIds.value(index.row());
            return store->value(id);
        }
        return this->pendingAdds()[index.row() - i];
    }

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
        return rowIds.length() + this->pendingAdds().length();
    };
};

#endif // POD_TABLE_MODEL_H
