#include "required.h"
#include "trimmed.h"
#include <QModelIndex>

RequiredValidatorFactory::RequiredValidatorFactory()
    : ValidatorFactory{false, true}
{}

const QString RequiredValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    auto message = trimmedValidatorFactory->isValid(index, value);
    if (!message.isEmpty()) return message;
    return value.isEmpty() ? tr("%1 is required").arg(columnHeader(index)) : nullptr;
}
