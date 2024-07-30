#include "unique.h"
#include "required.h"

UniqueValidatorFactory::UniqueValidatorFactory(int columnIndex, QList<int> groupingColumns)
    : ValidatorFactory(true)
    , columnIndex{columnIndex}, groupingColumns{groupingColumns}, values{}, model{nullptr} {}

const QString UniqueValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    auto message = requiredValidatorFactory->isValid(index, value);
    if (!message.isEmpty()) return message;
    auto rows = values.values(rowValues(value, index));
    return rows.length() > 1 || !rows.isEmpty() && !rows.contains(index.row()) ? formatMessage(tr("%1 must be unique"), index) : nullptr;
}

void UniqueValidatorFactory::initialize(QAbstractTableModel *model) {
    this->model = model;
    connect(model, SIGNAL(modelReset()), this, SLOT(modelReset()));
    connect(model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged(QModelIndex,QModelIndex,QList<int>)));
    connect(model, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(rowsInserted(QModelIndex,int,int)));
    connect(model, SIGNAL(rowsRemoved(QModelIndex,int,int)), this, SLOT(rowsRemoved(QModelIndex,int,int)));
}

void UniqueValidatorFactory::modelReset() {
    values.clear();
    if (model->rowCount() > 0) rowsInserted(QModelIndex{}, 0, model->rowCount()-1);
}

void UniqueValidatorFactory::dataChanged(const QModelIndex &topLeft, const QModelIndex &bottomRight, const QList<int> &roles) {
    for (int c = topLeft.column(); c <= bottomRight.column(); ++c) {
        if (c == columnIndex || groupingColumns.contains(c)) {
            rowsRemoved(QModelIndex{}, topLeft.row(), bottomRight.row());
            rowsInserted(QModelIndex{}, topLeft.row(), bottomRight.row());
            break;
        }
    }
}

void UniqueValidatorFactory::rowsInserted(const QModelIndex &parent, int first, int last) {
    for (int r = first; r <= last; ++r) {
        values.insert(rowValues(model->index(r, columnIndex)), r);
    }
}

void UniqueValidatorFactory::rowsRemoved(const QModelIndex &parent, int first, int last) {
    values.removeIf([=](auto iter) -> bool { return iter.value() >= first && iter.value() <= last; });
}

QStringList UniqueValidatorFactory::rowValues(const QModelIndex &index) const {
    return rowValues(index.data().toString(), index);
}

QStringList UniqueValidatorFactory::rowValues(const QString value, const QModelIndex &index) const {
    QStringList values{value.toLower()};
    for (int c : groupingColumns) {
        values.append(index.siblingAtColumn(c).data().toString());
    }
    return values;
}

#include "unique.moc"
