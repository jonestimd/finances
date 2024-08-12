#include "required.h"
#include "trimmed.h"
#include <QModelIndex>

RequiredValidatorFactory::RequiredValidatorFactory() {}

const QString RequiredValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    auto message = trimmedValidatorFactory->isValid(index, value);
    if (!message.isEmpty()) return message;
    return value.isEmpty() ? formatMessage(tr("%1 is required"), index): nullptr;
}
