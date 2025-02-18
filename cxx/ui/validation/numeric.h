#ifndef NUMEICR_H
#define NUMEICR_H

#include "validatorfactory.h"

class NumberValidatorFactory : public ValidatorFactory {
    typedef std::function<bool(const QModelIndex &)> IsRequired;

    QDoubleValidator validator;
    const IsRequired isRequired;
    const GetTitle getTitle;

public:
    NumberValidatorFactory(int decimals, bool required = false, GetTitle getTitle = ValidatorFactory::columnHeader);
    NumberValidatorFactory(IsRequired isRequired, int decimals, GetTitle getTitle = ValidatorFactory::columnHeader);

    const QString isValid(const QModelIndex &index, QString &value) const override;

    void fixup(QString &value) const override;
};

#endif // NUMEICR_H
