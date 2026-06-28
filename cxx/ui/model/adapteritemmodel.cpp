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
        if (isPendingAdd(index)) return finances::Add;
        if (changes.contains(index)) return finances::Update;
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
    if (pendingDeletes.removeOne(index.siblingAtColumn(0))) rowChanged(index);
    else if (changes.contains(index)) {
        changes.remove(index);
        emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
        revalidateRow(index);
    }
}

void AdapterItemModel::valuesAdded(const QList<domain_id> &ids) {
    for (auto id : ids) addRootId(id);
}

void AdapterItemModel::valuesToBeRemoved(const QList<domain_id> &ids) {
    for (auto id : ids) removeRootId(id);
}

bool AdapterItemModel::isPendingDelete(const QModelIndex &index) const {
    return pendingDeletes.contains(index.siblingAtColumn(0));
}

void AdapterItemModel::addRootId(domain_id id) {
    auto rowIndex = insertIndex(id);
    beginInsertRows(QModelIndex{}, rowIndex, rowIndex);
    rootIds.insert(rowIndex, id);
    updateIndexes(index(rowIndex, 0), 1);
    endInsertRows();
}

void AdapterItemModel::removeRootId(domain_id id) {
    auto rowIndex = rootIds.indexOf(id);
    if (rowIndex >= 0) {
        beginRemoveRows(QModelIndex{}, rowIndex, rowIndex);
        rootIds.remove(rowIndex);
        pendingDeletes.removeIf([=](const QModelIndex index) {
            auto row = index.parent().isValid() ? index.parent().row() : index.row();
            return row == rowIndex;
        });
        changes.removeIf([=](QHash<const QModelIndex, QVariant>::iterator i) -> bool {
            for (auto n = i.key(); n.isValid(); n = n.parent()) {
                if (!n.parent().isValid() && n.row() == rowIndex) return true;
            }
            return false;
        });
        updateIndexes(index(rowIndex, 0), -1);
        endRemoveRows();
    }
}

qsizetype AdapterItemModel::insertIndex(domain_id id) {
    return rootIds.size();
}

void AdapterItemModel::rowsChanged(int from, int to, const QModelIndex &parent) {
    emit dataChanged(index(from, 0, parent), index(to, columnCount(parent)-1, parent), QList<int>{Qt::DisplayRole, finances::UnsavedRole});
}

void AdapterItemModel::rowChanged(const QModelIndex &index) {
    rowsChanged(index.row(), index.row(), index.parent());
}

void AdapterItemModel::adjustErrorIndexes(const QModelIndex &changeRow, int delta) {
    QHash<const QModelIndex, QString> updatedErrors;
    for (auto [iIndex, message] : errors.asKeyValueRange()) {
        updatedErrors.insert(adjustIndex(iIndex, changeRow, delta), message);
    }
    errors = updatedErrors;
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

const QModelIndex AdapterItemModel::adjustIndex(const QModelIndex index, const QModelIndex& changeRow, int delta) {
    QList<QModelIndex> nodes;
    auto parent = changeRow.parent();
    for (auto i = index; i.isValid() && i != parent; i = i.parent()) nodes.append(i);
    auto newIndex = nodes.takeLast();
    if (newIndex.parent() != parent || newIndex.row() < changeRow.row()) return index;
    newIndex = newIndex.siblingAtRow(newIndex.row() + delta);
    while (!nodes.isEmpty()) {
        auto node = nodes.takeLast();
        newIndex = newIndex.model()->index(node.row(), node.column(), newIndex);
    }
    return newIndex;
}

void AdapterItemModel::updateDeletes(QList<QModelIndex>& pendingDeletes, const QModelIndex& changeRow, int delta) {
    for (auto i = pendingDeletes.begin(); i != pendingDeletes.end(); i++) {
        *i = adjustIndex(*i, changeRow, delta);
    }
}

void AdapterItemModel::updateIndexes(const QModelIndex& changeRow, int delta) {
    updateDeletes(pendingDeletes, changeRow, delta);
    changes = updateChanges(changes, changeRow, delta);
    adjustErrorIndexes(changeRow, delta);
}

QHash<const QModelIndex, QVariant> AdapterItemModel::updateChanges(const QHash<const QModelIndex, QVariant> &changes, const QModelIndex& changeRow, int delta) {
    QHash<const QModelIndex, QVariant> updatedChanges;
    for (auto [index, value] : changes.asKeyValueRange()) {
        updatedChanges.insert(adjustIndex(index, changeRow, delta), value);
    }
    return updatedChanges;
}

const QList<int> AdapterItemModel::rowIndexes(const QModelIndex &index) {
    QList<int> indexes{index.row()};
    auto parent = index.parent();
    while (parent.isValid()) {
        indexes.append(parent.row());
        parent = parent.parent();
    }
    return indexes;
}
