#include "accounttablemodel.h"

#include "amountcolumnadapter.h"
#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "formats.h"
#include "relationcolumnadapter.h"
#include "../validation/composite.h"
#include "../validation/required.h"
#include "../validation/trimmed.h"
#include "service/model/accounttype.h"

#define COMPANY_COLUMN 1
#define NAME_COLUMN 2

struct NameValidatorFactory : public ValidatorFactory {
    NameValidatorFactory() : ValidatorFactory(true) {}

    const QString isValid(const QModelIndex &index, QString &value) const override {
        QList<QString> values;
        auto model = index.model();
        auto companyName = index.siblingAtColumn(COMPANY_COLUMN).data();
        for (int r = 0; r < model->rowCount(); r++) {
            if (r != index.row() && index.sibling(r, COMPANY_COLUMN).data() == companyName) {
                auto other = index.sibling(r, index.column()).data();
                values.append(other.toString().toLower());
            }
        }
        return values.contains(value.trimmed().toLower()) ? formatMessage("%1 must be unique", index): nullptr;
    }

    const QString isValid(const QModelIndex &index) const {
        auto value = index.data().toString();
        return isValid(index, value);
    }
};

Q_GLOBAL_STATIC(CompositeValidatorFactory, nameValidatorFactory,
    QList<ValidatorFactory*>{requiredValidatorFactory, trimmedValidatorFactory, new NameValidatorFactory()}
)

struct CompanyValidatorFactory : public ValidatorFactory {
    const QString isValid(const QModelIndex &index, QString &value) const override {
        return nullptr;
    }

    QList<QModelIndex> revalidate(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const override {
        QModelIndex i = index.siblingAtColumn(NAME_COLUMN);
        return nameValidatorFactory->revalidate(errors, i);
    }
};

Q_GLOBAL_STATIC(CompanyValidatorFactory, companyValidatorFactory)

AccountTableModel::AccountTableModel(DataStore *ds, QObject *parent, AddCompany addCompany)
    : dataStore{ds}
    , PodTableModel<Account>{
        QList<ColumnAdapter<Account>*>{
            new ColumnAdapter<Account>(tr("Closed"), &Account::closed),
            new RelationColumnAdapter<Account, Company>(tr("Company"), &Account::companyId, std::bind(&DataStore::companies, ds), addCompany, companyValidatorFactory),
            new ColumnAdapter<Account>(tr("Name"), &Account::name, true, nameValidatorFactory),
            new EnumColumnAdapter<Account, AccountType>(tr("Type"), &Account::type, &AccountType::values, requiredValidatorFactory, true, &AccountType::isCompatible),
            new ColumnAdapter<Account>(tr("Description"), &Account::description, true, trimmedValidatorFactory),
            new ColumnAdapter<Account>(tr("Number"), &Account::accountNumber, true, trimmedValidatorFactory),
            new NumberColumnAdapter<Account>(tr("Transactions"), &Account::transactions),
            new AmountColumnAdapter<Account>(tr("Balance"), &Account::balance, accountBalance, false),
        },
        parent,
    }
{}
