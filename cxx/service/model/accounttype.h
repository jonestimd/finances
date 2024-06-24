#include <QHash>
#include <QString>

class AccountType {
public:
    const char *name;
    const bool security;

    AccountType();
    AccountType(const char* name, bool security);

    AccountType& operator=(AccountType &type);

    AccountType operator=(AccountType type);
};

static const QHash<QString, AccountType> accountTypes = QHash<QString, AccountType>{
    {QString("BANK"), AccountType("Bank", false)},
    {QString("BROKERAGE"), AccountType("Brokerage", true)},
    {QString("CASH"), AccountType("Cash", false)},
    {QString("CREDIT"), AccountType("Credit", false)},
    {QString("LOAN"), AccountType("Loan", false)},
    {QString("401K"), AccountType("401(K)", true)},
};
