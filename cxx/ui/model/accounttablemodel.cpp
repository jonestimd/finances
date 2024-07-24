#include "accounttablemodel.h"

#include "amountcolumnadapter.h"
#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "formats.h"
#include "relationcolumnadapter.h"
#include "../validation/required.h"
#include "../validation/trimmed.h"
#include "service/model/accounttype.h"

AccountTableModel::AccountTableModel(DataStore *ds, QObject *parent, AddCompany addCompany)
    : dataStore{ds}
    , PodTableModel<Account>{
        QList<ColumnAdapter<Account>*>{
            new ColumnAdapter<Account>(tr("Closed"), &Account::closed),
            new RelationColumnAdapter<Account, Company>(tr("Company"), &Account::companyId, std::bind(&DataStore::companies, ds), addCompany),
            new ColumnAdapter<Account>(tr("Name"), &Account::name, true, requiredValidatorFactory), // TODO unique company+name
            new EnumColumnAdapter<Account, AccountType>(tr("Type"), &Account::type, &AccountType::values, true, &AccountType::isCompatible),
            new ColumnAdapter<Account>(tr("Description"), &Account::description, true, trimmedValidatorFactory),
            new ColumnAdapter<Account>(tr("Number"), &Account::accountNumber, true, trimmedValidatorFactory),
            new NumberColumnAdapter<Account>(tr("Transactions"), &Account::transactions),
            new AmountColumnAdapter<Account>(tr("Balance"), &Account::balance, accountBalance, false),
        },
        parent,
    }
{}
