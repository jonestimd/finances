#ifndef TRIMMED_H
#define TRIMMED_H

#include "validatorfactory.h"
#include <QValidator>

class TrimmedValidatorFactory : public ValidatorFactory {
public:
    TrimmedValidatorFactory();

    const QString isValid(const QModelIndex &index, QString &value) const override;
    const QString isValid(const QModelIndex &index, QString &value, GetTitle getTitle) const;

    void fixup(QString &) const override;
};

Q_GLOBAL_STATIC(TrimmedValidatorFactory, trimmedValidatorFactory)

#endif // TRIMMED_H
