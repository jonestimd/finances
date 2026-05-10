#include "numeric.h"
#include "ui/validation/required.h"

NumberValidatorFactory::NumberValidatorFactory(int decimals, bool required, GetTitle getTitle)
    : NumberValidatorFactory([=](const QModelIndex &) { return required; }, decimals, getTitle)
{}

NumberValidatorFactory::NumberValidatorFactory(IsRequired isRequired, int decimals, GetTitle getTitle)
    : ValidatorFactory(false)
    , validator(-INFINITY, INFINITY, decimals)
    , isRequired{isRequired}
    , getTitle{getTitle}
{
    validator.setNotation(QDoubleValidator::StandardNotation);
}

const QString NumberValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    if (isRequired(index)) {
        auto message = requiredValidatorFactory->isValid(index, value, getTitle);
        if (!message.isEmpty()) return message;
    }
    int pos{0};
    if (validator.validate(value, pos) == QValidator::Invalid) return tr("%1 is invalid").arg(getTitle(index));
    return nullptr;
}

void NumberValidatorFactory::fixup(QString &value) const {
    validator.fixup(value);
}
