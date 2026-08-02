#ifndef NUMEICR_H
#define NUMEICR_H

#include "validatorfactory.h"

class NumberValidatorFactory : public ValidatorFactory {
    typedef std::function<bool(const QModelIndex &)> IsRequired;

    QDoubleValidator validator;
    const IsRequired isRequired;
    const bool inclusive;

public:
    NumberValidatorFactory(int decimals, bool required = false, double minValue = -INFINITY, bool inclusive = true);
    NumberValidatorFactory(IsRequired isRequired, int decimals, double minValue = -INFINITY, bool inclusive = true);

    const QString isValid(const QModelIndex &index, QString &value) const override;

    void fixup(QString &value) const override;
};

#endif // NUMEICR_H
