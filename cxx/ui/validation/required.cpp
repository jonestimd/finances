#include "required.h"
#include <QModelIndex>

RequiredValidatorFactory::RequiredValidatorFactory() {}

const QString RequiredValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    return value.trimmed().isEmpty() ? formatMessage("%1 is required", index): nullptr;
}
