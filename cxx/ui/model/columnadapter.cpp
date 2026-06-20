#include "columnadapter.h"

AbstractColumnAdapter::AbstractColumnAdapter(ValidatorFactory *factory, const QString title)
    : validatorFactory{factory}, title{title}
{}

AbstractColumnAdapter::~AbstractColumnAdapter() {
    if (validatorFactory && !validatorFactory->global) delete validatorFactory;
}

void AbstractColumnAdapter::initialize(QAbstractItemModel *model) {
    if (validatorFactory) validatorFactory->initialize(model);
}

QVariant AbstractColumnAdapter::parseValue(const QVariant &value) {
    return value;
}

bool AbstractColumnAdapter::isEqual(const QVariant &value1, const QVariant &value2) const {
    return is_eq(QVariant::compare(value1, value2));
}

const QString AbstractColumnAdapter::isValid(const QModelIndex &index) const {
    if (validatorFactory) {
        auto val = index.data(Qt::EditRole).toString();
        return validatorFactory->isValid(index, val);
    }
    return nullptr;
}

QList<QModelIndex> AbstractColumnAdapter::revalidateRows(QHash<const QModelIndex, QString> &errors, const QModelIndex &index) const {
    if (validatorFactory) return validatorFactory->revalidateRows(errors, index);
    return QList<QModelIndex>{};
}
