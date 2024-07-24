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
    QList<int> pendingDeletes;

    QVariant value_(const QModelIndex index, int role = Qt::DisplayRole, QVariant current = QVariant{}) const {
        return columns[index.column()]->value(row(index.row()), current, role);
    }

    void emitChange(int from, int to) {
        emit dataChanged(index(from, 0), index(to, columns.length()-1), QList<int>{Qt::DisplayRole, finances::UnsavedRole});
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

    ~PodTableModel() {
        for (auto column : columns) {
            delete column;
        }
    }

    void setRows(QList<const Row*> rows) {
        this->clearChanges();
        beginResetModel();
        this->rows.clear();
        this->rows.append(rows);
        endResetModel();
    }

    const Row *row(int rowIndex) const {
        auto i = rows.length();
        return rowIndex < i ? rows[rowIndex] : pendingAdds[rowIndex - i];
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

    void queueDelete(int rowIndex) override {
        if (rowIndex >= rows.length()) {
            beginRemoveRows(QModelIndex{}, rowIndex, rowIndex);
            delete pendingAdds.takeAt(rowIndex - rows.length());
            QHash<QModelIndex, QString> updateErrors;
            for (auto [key, value] : errors.asKeyValueRange()) {
                if (key.row() < rowIndex) updateErrors[key] = value;
                else if (key.row() > rowIndex) updateErrors[key.siblingAtRow(key.row()-1)] = value;
            }
            errors.clear();
            errors.insert(updateErrors);
            endRemoveRows();
        }
        else if (!pendingDeletes.contains(rowIndex)) {
            pendingDeletes.append(rowIndex);
            emitChange(rowIndex);
        }
    }

    void undoChange(const QModelIndex &index) override {
        if (pendingDeletes.removeAll(index.row()) > 0) emitChange(index.row());
        else if (changes.contains(index)) {
            changes.remove(index);
            emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
        }
    }

    int columnIndex(const QString name) const override {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    bool hasUnsavedChanges() {
        return !changes.isEmpty() || !pendingAdds.isEmpty() || !pendingDeletes.isEmpty();
    }

    bool isValid() {
        return errors.isEmpty();
    }

    const QList<Row*> unsavedChanges() {
        QHash<int, Row*> changeRows;
        for (auto i = changes.cbegin(), end = changes.cend(); i != end; ++i) {
            auto row = i.key().row();
            if (pendingDeletes.contains(row)) continue;
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

    const QList<const Row*> unsavedDeletes() const {
        QList<const Row*> deletes;
        for (auto i : pendingDeletes) deletes.append(rows[i]);
        return deletes;
    }

    void clearChanges() {
        changes.clear();
        pendingDeletes.clear();
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
            if (changes.contains(index)) return value_(index, role, changes.value(index));
            break;
        case finances::ValidationMessageRole:
            if (errors.contains(index)) return errors[index];
            break;
        case finances::UnsavedRole:
            if (pendingDeletes.contains(index.row())) return finances::Delete;
            if (index.row() >= rows.length() || changes.contains(index)) return finances::AddUpdate;
        }
        return value_(index, role);
    }

    Qt::ItemFlags flags(const QModelIndex &index) const override {
        bool pendingDelete = pendingDeletes.contains(index.row());
        return AdapterTableModel::flags(index) | columns[index.column()]->flags(row(index.row()), !pendingDelete);
    }

    bool setData(const QModelIndex &index, const QVariant &value, int role) override {
        if (role == Qt::EditRole) {
            errors.remove(index); // editor must have accepted the value
            auto savedRows = rows.length();
            if (index.row() >= savedRows) {
                setValue_(pendingAdds[index.row()-savedRows], index.column(), value);
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole));
                return true;
            }
            auto column = columns[index.column()];
            auto original = value_(index, Qt::EditRole);
            if (column->isEqual(original, value)) {
                if (changes.contains(index)) {
                    changes.remove(index);
                    emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
                    return true;
                }
            } else if (!changes.contains(index) || !column->isEqual(value, changes[index])) {
                changes[index] = value;
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
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
