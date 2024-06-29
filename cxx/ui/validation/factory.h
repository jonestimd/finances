#ifndef FACTORY_H
#define FACTORY_H

#include <QStatusBar>
#include <QValidator>

class ValidatorFactory : public QObject {
    Q_OBJECT
public:
    ValidatorFactory() {}

    virtual const QValidator *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) = 0;
};

Q_DECLARE_OPAQUE_POINTER(ValidatorFactory*)

#endif // FACTORY_H
