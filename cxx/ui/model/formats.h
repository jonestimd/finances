#ifndef FORMATS_H
#define FORMATS_H

#include "service/model/account.h"
#include "service/model/security.h"
#include <QVariant>

QString accountBalance(const Account *row, const QVariant &balance);
QString securityDollarAmount(const Security *row, const QVariant &amount);
QString securityShares(const Security *row, const QVariant &amount);
QString formatDate(const QVariant &value);

#endif // FORMATS_H
