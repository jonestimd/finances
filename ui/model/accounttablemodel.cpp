#include "accounttablemodel.h"

#include "amountcolumnadapter.h"
#include "boolcolumnadapter.h"
#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "formats.h"
#include "relationcolumnadapter.h"
#include "../../database/model/accounttype.h"

AccountTableModel::AccountTableModel(QList<Company*> companies, QList<Account*> accounts, QObject *parent)
    : PodTableModel<Account>{
        QList<ColumnAdapter<Account>*>{
            new BoolColumnAdapter<Account>(tr("Closed"), &Account::closed),
            new RelationColumnAdapter<Account, Company>(tr("Company"), &Account::companyId, companies),
            new ColumnAdapter<Account>(tr("Name"), &Account::name),
            new EnumColumnAdapter<Account, AccountType>(tr("Type"), &Account::type, accountTypes),
            new ColumnAdapter<Account>(tr("Description"), &Account::description),
            new ColumnAdapter<Account>(tr("Number"), &Account::accountNumber),
            new NumberColumnAdapter<Account>(tr("Transactions"), &Account::transactions),
            new AmountColumnAdapter<Account>(tr("Balance"), &Account::balance, accountBalance),
        }
    }
{
    setRows(accounts);
}
