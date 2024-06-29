#ifndef UNIQUE_H
#define UNIQUE_H

#include "factory.h"
#include <QModelIndex>
#include <QValidator>

class UniqueValidatorFactory : public ValidatorFactory {
public:
    UniqueValidatorFactory();

    const QValidator *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) override;
};

Q_GLOBAL_STATIC(UniqueValidatorFactory, uniqueValidatorFactory)

#endif // UNIQUE_H
