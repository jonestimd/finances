#include "adapteritemmodel.h"
#include "ui/finances.h"

AdapterItemModel::AdapterItemModel(QObject *parent) : QAbstractItemModel{parent} {}

bool AdapterItemModel::hasUnsavedChanges() const {
    return !changes.isEmpty() || !pendingDeletes.isEmpty();
}

void AdapterItemModel::clearChanges() {
    changes.clear(); // TODO emit changes
    pendingDeletes.clear(); // TODO emit changes
    errors.clear();
}

bool AdapterItemModel::isValid() const {
    return errors.isEmpty();
}

QVariant AdapterItemModel::data(const QModelIndex &index, int role) const {
    switch (role) {
    case Qt::DisplayRole:
    case Qt::EditRole:
        return value(index, role, changes.value(index));
    case finances::ValidationMessageRole:
        return errors.contains(index) ? errors.value(index) : QVariant{};
    case finances::UnsavedRole:
        if (isPendingDelete(index)) return finances::Delete;
        if (isPendingAdd(index) || changes.contains(index)) return finances::AddUpdate;
        return QVariant{};
    }
    return value(index, role, changes.value(index));
}

bool AdapterItemModel::setData(const QModelIndex &index, const QVariant &value, int role) {
    if (role == Qt::EditRole) {
        errors.remove(index); // editor must have accepted the value
        auto column = adapter(index);
        auto parsed = column->parseValue(value);
        if (isPendingAdd(index)) {
            setValue(index, parsed);
            emit dataChanged(index, index, QList<int>(Qt::DisplayRole));
            revalidateRow(index);
            return true;
        }
        auto original = this->value(index, Qt::EditRole);
        if (column->isEqual(original, parsed)) {
            if (changes.contains(index)) {
                changes.remove(index);
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
                revalidateRow(index);
                return true;
            }
        } else if (!changes.contains(index) || !column->isEqual(parsed, changes[index])) {
            changes.insert(index, parsed);
            emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
            revalidateRow(index);
            return true;
        }
    }
    return false;
}

QVariant AdapterItemModel::headerData(int section, Qt::Orientation orientation, int role) const {
    if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section >= 0 && section < columnCount()) {
        return adapter(createIndex(0, section))->title;
    }
    return QVariant{};
}

void AdapterItemModel::queueDelete(const QModelIndex &index) {
    if (!isPendingDelete(index)) {
        pendingDeletes.append(index.siblingAtColumn(0));
        rowChanged(index);
    }
}

void AdapterItemModel::undoChange(const QModelIndex &index) {
    if (pendingDeletes.removeAll(index.siblingAtColumn(0)) > 0) rowChanged(index);
    else if (changes.contains(index)) {
        changes.remove(index);
        emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
        revalidateRow(index);
    }
}

bool AdapterItemModel::isPendingDelete(const QModelIndex &index) const {
    return pendingDeletes.contains(index.siblingAtColumn(0));
}

void AdapterItemModel::rowsChanged(int from, int to, const QModelIndex &parent) {
    emit dataChanged(index(from, 0, parent), index(to, columnCount(parent)-1, parent), QList<int>{Qt::DisplayRole, finances::UnsavedRole});
}

void AdapterItemModel::rowChanged(const QModelIndex &index) {
    rowsChanged(index.row(), index.row(), index.parent());
}

void AdapterItemModel::adjustErrorIndexes(int rowIndex, const QModelIndex &parent, int delta) {
    QHash<QModelIndex, QString> updateErrors;
    for (auto [key, value] : errors.asKeyValueRange()) {
        if (key.parent() != parent || key.row() < rowIndex) updateErrors.insert(key, value);
        else if (key.row() > rowIndex) updateErrors.insert(key.siblingAtRow(key.row() + delta), value);
    }
    errors.clear();
    errors.insert(updateErrors);
}

const QString AdapterItemModel::validate(const QModelIndex &index) {
    return adapter(index)->isValid(index);
}

void AdapterItemModel::validateRow(int rowIndex, const QModelIndex &parent) {
    auto columns = columnCount(parent.parent());
    for (int colIndex = 0; colIndex < columns; ++colIndex) {
        auto i = index(rowIndex, colIndex, parent);
        auto message = validate(i);
        if (!message.isNull()) errors.insert(i, message);
    }
}

void AdapterItemModel::removeStaleErrors() {
    QList<QModelIndex> fixes;
    for (auto [index, message] : errors.asKeyValueRange()) {
        auto newMessage = validate(index);
        if (newMessage.isNull()) fixes.append(index);
    }
    for (auto index : fixes) {
        errors.remove(index);
        emit dataChanged(index, index, QList<int>{finances::ValidationMessageRole});
    }
}

void AdapterItemModel::revalidateColumn(int column, const QModelIndex &parent) {
    const auto changes = adapter(index(0, column, parent))->revalidateRows(errors, index(0, column, parent));
    for (auto index : changes) {
        emit dataChanged(index, index, QList<int>{finances::ValidationMessageRole});
    }
}

void AdapterItemModel::revalidateRow(const QModelIndex &index) {
    auto parent = index.parent();
    auto columns = columnCount(parent);
    for (int c = 0; c < columns; ++c) {
        auto i = index.siblingAtColumn(c);
        auto message = validate(i);
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
            revalidateColumn(c, parent);
        }
    }
}

const QList<int> AdapterItemModel::rowIndexes(const QModelIndex &index) const {
    QList<int> indexes{index.row()};
    auto parent = index.parent();
    while (parent.isValid()) {
        indexes.append(parent.row());
        parent = parent.parent();
    }
    return indexes;
}
