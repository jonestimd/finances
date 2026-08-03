#include "numeric.h"
#include "ui/validation/required.h"

NumberValidatorFactory::NumberValidatorFactory(int decimals, bool required, double minValue, bool inclusive)
    : NumberValidatorFactory([=](const QModelIndex &) { return required; }, decimals, minValue, inclusive)
{}

NumberValidatorFactory::NumberValidatorFactory(IsRequired isRequired, int decimals, double minValue, bool inclusive)
    : ValidatorFactory(false)
    , validator(minValue, INFINITY, decimals)
    , isRequired{isRequired}
    , inclusive{inclusive}
{
    validator.setNotation(QDoubleValidator::StandardNotation);
}

const QString NumberValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    if (isRequired(index)) {
        auto message = requiredValidatorFactory->isValid(index, value);
        if (!message.isEmpty()) return message;
    }
    int pos{0};
    if (validator.validate(value, pos) == QValidator::Invalid) return tr("%1 is invalid").arg(columnHeader(index));
    if (!inclusive && value.toDouble() == validator.bottom()) {
        return tr("%1 must be greater that %2").arg(columnHeader(index)).arg(validator.bottom());
    }
    return nullptr;
}

void NumberValidatorFactory::fixup(QString &value) const {
    validator.fixup(value);
}
