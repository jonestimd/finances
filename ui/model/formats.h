#ifndef FORMATS_H
#define FORMATS_H

#include "../../database/model/account.h"
#include <QVariant>

QString accountBalance(const Account *row, QVariant balance);

#endif // FORMATS_H
