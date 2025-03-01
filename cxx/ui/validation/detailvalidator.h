#ifndef DETAILVALIDATOR_H
#define DETAILVALIDATOR_H

#include "numeric.h"

namespace detailvalidator {
    const QString getDetailTitle(const QModelIndex &index);
}

class SharesValidatorFactory : public NumberValidatorFactory {
    const int categoryColumnIndex;
    const int securityColumnIndex;
    const int amountColumnIndex;

public:
    SharesValidatorFactory(int categoryColumnIndex, int securityColumnIndex, int amountColumnIndex);

    const QString isValid(const QModelIndex &index, QString &value) const override;
    bool isRequired(const QModelIndex &index) const;

private:
    bool isTransfer(const QModelIndex &index) const;
};

class DetailAmountValidatorFactory : public NumberValidatorFactory {
public:
    DetailAmountValidatorFactory();

    bool isRequired(const QModelIndex &index) const;
};

#endif // DETAILVALIDATOR_H
