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
        return columns[index.column()]->value(row(index.row()), index, current, role);
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

    void removeStaleErrors() {
        QList<QModelIndex> fixes;
        for (auto [index, message] : errors.asKeyValueRange()) {
            auto newMessage = columns[index.column()]->isValid(index);
            if (newMessage.isNull()) fixes.append(index);
        }
        for (auto index : fixes) {
            errors.remove(index);
            emit dataChanged(index, index, QList<int>{finances::ValidationMessageRole});
        }
    }

    void revalidateColumn(int column) {
        auto changes = columns[column]->revalidate(errors, index(0, column));
        for (auto index : changes) {
            emit dataChanged(index, index, QList<int>{finances::ValidationMessageRole});
        }
    }

    void revalidateRow(int rowIndex) {
        for (int c = 0; c < columns.length(); ++c) {
            auto i = index(rowIndex, c);
            auto message = columns[c]->isValid(i);
            if (message.isEmpty()) {
                if (errors.remove(i)) {
                    emit dataChanged(i, i, QList<int>{finances::ValidationMessageRole});
                    removeStaleErrors();
                }
            } else {
                if (!errors.contains(i) || message != errors.value(i)) {
                    errors.insert(i, message);
                    emit dataChanged(i, i, QList<int>{finances::ValidationMessageRole});
                }
                // uniqueness conflict may have moved to another row
                revalidateColumn(c);
            }
        }
    }

public:
    explicit PodTableModel(const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : AdapterTableModel(parent), rows(QList<const Row*>()), columns{columns}
    {
        for (auto column : columns) {
            column->initialize(this);
        }
    }

    ~PodTableModel() {
        for (auto column : columns) delete column;
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
        for (int colIndex = 0; colIndex < columns.length(); ++colIndex) {
            auto i = index(rowIndex, colIndex);
            auto message = columns[colIndex]->isValid(i);
            if (!message.isNull()) errors[i] = message;
        }
        endInsertRows();
        return rowIndex;
    }

    bool enableDelete(int rowIndex) const override {
        return row(rowIndex)->deletable();
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
            removeStaleErrors();
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
            revalidateRow(index.row());
        }
    }

    int columnIndex(const QString name) const override {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    virtual bool hasUnsavedChanges() const override {
        return !changes.isEmpty() || !pendingAdds.isEmpty() || !pendingDeletes.isEmpty();
    }

    virtual bool isValid() const override {
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

    virtual void clearChanges() override {
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
                revalidateRow(index.row());
                return true;
            }
            auto column = columns[index.column()];
            auto original = value_(index, Qt::EditRole);
            if (column->isEqual(original, value)) {
                if (changes.contains(index)) {
                    changes.remove(index);
                    emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
                    revalidateRow(index.row());
                    return true;
                }
            } else if (!changes.contains(index) || !column->isEqual(value, changes[index])) {
                changes[index] = value;
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
                revalidateRow(index.row());
                return true;
            }
        }
        return false;
    }

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override {
        if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section >= 0 && section < columns.count()) {
            return columns[section]->title;
        }
        return QVariant{};
    }
};

#endif // PODTABLEMODEL_H
