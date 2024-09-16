#ifndef POD_TABLE_MODEL_H
#define POD_TABLE_MODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "poditemmodel.h"
#include "columnadapter.h"

template<Copyable Row>
class PodTableModel : public PodItemModel<Row>
{
protected:
    QList<const Row*> rows;

    int childCount(const QModelIndex &index) const override {
        return index.isValid() ? 0 : rows.length();
    }

public:
    explicit PodTableModel(const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : PodItemModel<Row>(columns, parent), rows(QList<const Row*>())
    {}

    void setRows(QList<const Row*> rows) {
        this->clearChanges();
        this->beginResetModel();
        this->rows.clear();
        this->rows.append(rows);
        this->endResetModel();
    }

    const Row *getRow(const QModelIndex &index) const override {
        auto i = rows.length();
        return index.row() < i ? rows[index.row()] : this->pendingAdds()[index.row() - i];
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
        return rows.length() + this->pendingAdds().length();
    };
};

#endif // POD_TABLE_MODEL_H
