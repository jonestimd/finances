#include "accounttablemodel.h"

#include "amountcolumnadapter.h"
#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "formats.h"
#include "relationcolumnadapter.h"
#include "../validation/required.h"
#include "../../service/model/accounttype.h"

AccountTableModel::AccountTableModel(DataStore *ds, QObject *parent)
    : dataStore{ds},
    PodTableModel<Account>{
        QList<ColumnAdapter<Account>*>{
            new ColumnAdapter<Account>(tr("Closed"), &Account::closed),
            new RelationColumnAdapter<Account, Company>(tr("Company"), &Account::companyId, std::bind(&DataStore::companies, ds)),
            new ColumnAdapter<Account>(tr("Name"), &Account::name, true, requiredValidatorFactory),
            new EnumColumnAdapter<Account, AccountType>(tr("Type"), &Account::type, accountTypes),
            new ColumnAdapter<Account>(tr("Description"), &Account::description),
            new ColumnAdapter<Account>(tr("Number"), &Account::accountNumber),
            new NumberColumnAdapter<Account>(tr("Transactions"), &Account::transactions),
            new AmountColumnAdapter<Account>(tr("Balance"), &Account::balance, accountBalance, false),
        },
        parent,
    }
{}
