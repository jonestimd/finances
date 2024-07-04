#ifndef PODTABLEMODEL_H
#define PODTABLEMODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "adaptertablemodel.h"
#include "columnadapter.h"

template<class Row>
concept Copyable = requires(Row &t){
    { new Row() } -> std::convertible_to<Row*>;
    { new Row(t) } -> std::convertible_to<Row*>;
};

template<Copyable Row>
class PodTableModel : public AdapterTableModel
{
protected:
    const QList<ColumnAdapter<Row>*> columns;
    QHash<QModelIndex, QVariant> changes;
    QHash<QModelIndex, QString> errors;
    QList<const Row*> rows;
    QList<Row*> pendingAdds;

    const Row *row_(const QModelIndex index) const {
        auto r = index.row(), i = rows.length();
        return r < i ? rows[r] : pendingAdds[r - i];
    }

    QVariant value_(const QModelIndex index, int role = Qt::DisplayRole) const {
        return columns[index.column()]->value(row_(index), role);
    }

    void emitChange(int from, int to) {
        emit dataChanged(index(from, 0), index(to, columns.length()-1), QList<int>{Qt::DisplayRole, finances::Unsaved});
    }

    void emitChange(int row) {
        emitChange(row, row);
    }

    void setValue_(Row *row, int column, QVariant value) {
        columns[column]->setValue(row, value);
    }

public:
    explicit PodTableModel(const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : AdapterTableModel(parent), rows(QList<const Row*>()), columns{columns} {}

    void setRows(QList<const Row*> rows) {
        beginResetModel();
        this->rows.clear();
        this->rows.append(rows);
        this->clearChanges();
        endResetModel();
    }

    int queueAdd() override {
        auto rowIndex = rowCount();
        beginInsertRows(QModelIndex{}, rowIndex, rowIndex);
        Row *row = new Row;
        pendingAdds.append(row);
        QObject deleter;
        for (int colIndex = 0; colIndex < columns.length(); ++colIndex) {
            auto i = index(rowIndex, colIndex);
            auto message = columns[colIndex]->isValid(row, i, &deleter);
            if (!message.isNull()) errors[i] = message;
        }
        endInsertRows();
        emitChange(rowIndex);
        return rowIndex;
    }

    int columnIndex(const QString name) const override {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    bool hasUnsavedChanges() {
        return !changes.isEmpty() || !pendingAdds.isEmpty();
    }

    bool isValid() {
        return errors.isEmpty();
    }

    const QList<Row*> unsavedChanges() {
        QHash<int, Row*> changeRows;
        for (auto i = changes.cbegin(), end = changes.cend(); i != end; ++i) {
            auto row = i.key().row();
            Row *updated;
            if (changeRows.contains(row)) updated = changeRows[row];
            else {
                updated = new Row(*rows[row]);
                changeRows[row] = updated;
            }
            setValue_(updated, i.key().column(), i.value());
        }
        return changeRows.values();
    }

    const QList<Row*> unsavedAdds() const {
        QList<Row*> rows;
        for (auto row : pendingAdds) {
            rows.append(new Row(*row));
        }
        return rows;
    }

    void clearChanges() {
        changes.clear();
        errors.clear();
        if (!pendingAdds.isEmpty()) {
            beginRemoveRows(QModelIndex{}, rows.length(), rowCount()-1);
            while (!pendingAdds.isEmpty()) delete pendingAdds.takeFirst();
            endRemoveRows();
        }
        emitChange(0, rowCount()-1);
    }

    // QAbstractItemModel interface
    int rowCount(const QModelIndex &parent = QModelIndex()) const override {
        if (parent.isValid()) return 0;
        return rows.length() + pendingAdds.length();
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
        case finances::ValidationMessage:
            if (errors.contains(index)) return errors[index];
            break;
        case finances::Unsaved:
            return index.row() >= rows.length() || changes.contains(index);
        }
        return value_(index, role);
    }

    Qt::ItemFlags flags(const QModelIndex &index) const override {
        return AdapterTableModel::flags(index) | columns[index.column()]->flags(row_(index));
    }

    bool setData(const QModelIndex &index, const QVariant &value, int role) override {
        if (role == Qt::EditRole) {
            errors.remove(index); // editor must have accepted the value
            auto text = value.toString().trimmed();
            auto savedRows = rows.length();
            if (index.row() >= savedRows) {
                setValue_(pendingAdds[index.row()-savedRows], index.column(), value);
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole));
                return true;
            }
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
