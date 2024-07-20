#ifndef TRIMMED_H
#define TRIMMED_H

#include "factory.h"
#include <QValidator>

class TrimmedValidatorFactory : public ValidatorFactory {
public:
    TrimmedValidatorFactory();

    const ValidationStatus *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) const override;
};

Q_GLOBAL_STATIC(TrimmedValidatorFactory, trimmedValidatorFactory)

#endif // TRIMMED_H
