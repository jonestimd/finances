#include "accounttablemodel.h"

#include "amountcolumnadapter.h"
#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "formats.h"
#include "relationcolumnadapter.h"
#include "../validation/required.h"
#include "../validation/trimmed.h"
#include "../validation/unique.h"
#include "service/model/accounttype.h"

#define COMPANY_COLUMN 1
#define NAME_COLUMN 2

class AccountValidatorFactory : public UniqueValidatorFactory {
public:
    AccountValidatorFactory() : UniqueValidatorFactory{NAME_COLUMN} {}

protected:
    virtual QStringList rowValues(const QString value, const QModelIndex &index) const override {
        auto values = UniqueValidatorFactory::rowValues(value, index);
        values.append(index.siblingAtColumn(COMPANY_COLUMN).data().toString());
        return values;
    }

    virtual bool isValidated(int columnIndex) const override {
        return UniqueValidatorFactory::isValidated(columnIndex) || columnIndex == COMPANY_COLUMN;
    }
};

AccountTableModel::AccountTableModel(DataStore *ds, QObject *parent, AddCompany addCompany)
    : PodTableModel<Account>{
        ds->accounts(),
        QList<ColumnAdapter<Account>*>{
            new ColumnAdapter<Account>(tr("Closed"), &Account::closed),
            new RelationColumnAdapter<Account, Company>(tr("Company"), &Account::companyId, ds->companies(), addCompany),
            new ColumnAdapter<Account>(tr("Name"), &Account::name, true, new AccountValidatorFactory()),
            new EnumColumnAdapter<Account, AccountType>(tr("Type"), &Account::type, &AccountType::values, requiredValidatorFactory, true, &AccountType::isCompatible),
            new ColumnAdapter<Account>(tr("Description"), &Account::description, true, trimmedValidatorFactory),
            new ColumnAdapter<Account>(tr("Number"), &Account::accountNumber, true, trimmedValidatorFactory),
            new NumberColumnAdapter<Account>(tr("Transactions"), &Account::transactions),
            new AmountColumnAdapter<Account>(tr("Balance"), &Account::balance, accountBalance, false),
        },
        parent,
    },
    dataStore{ds}
{}

void AccountTableModel::companiesLoaded(const QList<qlonglong> companyIds) {
    for (auto [parentIndex, children] : newRows.asKeyValueRange()) {
        for (qsizetype i = 0; i < children.length(); i++) {
            auto account = children[i];
            if (!companyIds.contains(account->companyId.toLongLong())) {
                setData(index(rowIds.length() + i, COMPANY_COLUMN, parentIndex), QVariant{}, Qt::EditRole);
            }
        }
    }
    for (auto [index, value] : changes.asKeyValueRange()) {
        if (index.column() == COMPANY_COLUMN && !companyIds.contains(value.toLongLong())) {
            undoChange(index);
        }
    }
}
