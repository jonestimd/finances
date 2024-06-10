#ifndef PODTABLEMODEL_H
#define PODTABLEMODEL_H

#include <QAbstractTableModel>
#include <QBrush>
#include <QDecNumber.hh>
#include "columnadapter.h"

template<class Row>
class PodTableModel : public QAbstractTableModel
{
protected:
    QList<ColumnAdapter<Row>*> columns;
    QList<Row*> rows;

    void setRows(QList<Row*> rows) {
        this->rows = rows;
    }
public:
    explicit PodTableModel(QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : QAbstractTableModel(parent), columns{columns} {}

    int columnIndex(QString name) const {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    // QAbstractItemModel interface
    int rowCount(const QModelIndex &parent) const {
        if (parent.isValid()) return 0;
        return rows.count();
    };

    int columnCount(const QModelIndex &parent) const {
        if (parent.isValid()) return 0;
        return columns.count();
    }

    QVariant data(const QModelIndex &index, int role) const {
        return columns[index.column()]->value(rows[index.row()], role);
    }

    // bool setData(const QModelIndex &index, const QVariant &value, int role);

    QVariant headerData(int section, Qt::Orientation orientation, int role) const {
        if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section < columns.count()) {
            return columns[section]->title;
        }
        return QVariant{};
    }
};

#endif // PODTABLEMODEL_H
