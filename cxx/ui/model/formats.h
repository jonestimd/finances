#ifndef FORMATS_H
#define FORMATS_H

#include "../../service/model/account.h"
#include <QVariant>

QString accountBalance(const Account *row, QVariant balance);

#endif // FORMATS_H
