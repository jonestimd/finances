#ifndef PODTABLEMODEL_H
#define PODTABLEMODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "adaptertablemodel.h"
#include "columnadapter.h"

template<class Row>
concept Copyable = requires(Row &t){
    { new Row(t) } -> std::convertible_to<Row*>;
};

template<Copyable Row>
class PodTableModel : public AdapterTableModel
{
protected:
    const QList<ColumnAdapter<Row>*> columns;
    QHash<QModelIndex, QVariant> changes;
    QList<Row*> rows;

    QVariant value_(const QModelIndex index, int role = Qt::DisplayRole) const {
        return columns[index.column()]->value(rows[index.row()], role);
    }

public:
    explicit PodTableModel(const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : AdapterTableModel(parent), rows(QList<Row*>()), columns{columns} {}

    void setRows(QList<Row*> rows) {
        beginResetModel();
        this->rows.clear();
        this->rows.append(rows);
        this->clearChanges();
        endResetModel();
    }

    void setValue(Row *row, int column, QVariant value) {
        columns[column]->setValue(row, value);
    }

    int columnIndex(const QString name) const override {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    bool hasUnsavedChanges() {
        return !changes.isEmpty();
    }

    QList<Row*> unsavedChanges() {
        QHash<int, Row*> changeRows;
        for (auto i = changes.cbegin(), end = changes.cend(); i != end; ++i) {
            auto row = i.key().row();
            Row *updated;
            if (changeRows.contains(row)) updated = changeRows[row];
            else {
                updated = new Row(*rows[row]);
                changeRows[row] = updated;
            }
            setValue(updated, i.key().column(), i.value());
        }
        return changeRows.values();
    }

    void clearChanges() {
        changes.clear();
        emit dataChanged(index(0, 0), index(rows.length()-1, columns.length()-1), QList<int>(Qt::DisplayRole, finances::Unsaved));
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
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
            if (changes.contains(index)) return changes[index];
            break;
        case finances::Unsaved:
            return changes.contains(index);
        }
        return value_(index, role);
    }

    Qt::ItemFlags flags(const QModelIndex &index) const override {
        return AdapterTableModel::flags(index) | columns[index.column()]->flags(rows[index.row()]);
    }

    bool setData(const QModelIndex &index, const QVariant &value, int role) override {
        if (role == Qt::EditRole) {
            auto text = value.toString().trimmed();
            if (is_eq(QVariant::compare(value_(index), text))) {
                if (changes.contains(index)) {
                    changes.remove(index);
                    emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::Unsaved));
                    return true;
                }
            } else if (!changes.contains(index) || is_neq(QVariant::compare(text, changes[index]))) {
                changes[index] = text;
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::Unsaved));
                return true;
            }
        }
        return false;
    }

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override {
        if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section < columns.count()) {
            return columns[section]->title;
        }
        return QVariant{};
    }
};

#endif // PODTABLEMODEL_H
