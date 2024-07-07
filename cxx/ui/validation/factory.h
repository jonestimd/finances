#ifndef FACTORY_H
#define FACTORY_H

#include "status.h"
#include <QStatusBar>
#include <QValidator>

class ValidatorFactory : public QObject {
    Q_OBJECT
public:
    ValidatorFactory() {}

    virtual const ValidationStatus *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) const = 0;
};

Q_DECLARE_OPAQUE_POINTER(ValidatorFactory*)

#endif // FACTORY_H
