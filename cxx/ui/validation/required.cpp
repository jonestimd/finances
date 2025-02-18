#include "required.h"
#include "trimmed.h"
#include <QModelIndex>

RequiredValidatorFactory::RequiredValidatorFactory()
    : ValidatorFactory{false, true}
{}

const QString RequiredValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    return isValid(index, value, columnHeader);
}

const QString RequiredValidatorFactory::isValid(const QModelIndex &index, QString &value, GetTitle getTitle) const {
    auto message = trimmedValidatorFactory->isValid(index, value, getTitle);
    if (!message.isEmpty()) return message;
    return value.isEmpty() ? tr("%1 is required").arg(getTitle(index)) : nullptr;
}
