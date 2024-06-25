#ifndef PODTABLEMODEL_H
#define PODTABLEMODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "adaptertablemodel.h"
#include "columnadapter.h"

template<class Row>
class PodTableModel : public AdapterTableModel
{
protected:
    const QList<ColumnAdapter<Row>*> columns;
    QList<Row*> rows;

public:
    explicit PodTableModel(const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : AdapterTableModel(parent), rows(QList<Row*>()), columns{columns} {}

    void setRows(QList<Row*> rows) {
        beginResetModel();
        this->rows.clear();
        this->rows.append(rows);
        endResetModel();
    }

public:
    int columnIndex(const QString name) const override {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    // QAbstractItemModel interface
    int rowCount(const QModelIndex &parent) const override {
        if (parent.isValid()) return 0;
        return rows.count();
    };

    int columnCount(const QModelIndex &parent) const override {
        if (parent.isValid()) return 0;
        return columns.count();
    }

    QVariant data(const QModelIndex &index, int role) const override {
        return columns[index.column()]->value(rows[index.row()], role);
    }

    // bool setData(const QModelIndex &index, const QVariant &value, int role) override;

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override {
        if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section < columns.count()) {
            return columns[section]->title;
        }
        return QVariant{};
    }
};

#endif // PODTABLEMODEL_H
