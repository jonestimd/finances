#include "accounttablemodel.h"

#include "amountcolumnadapter.h"
#include "boolcolumnadapter.h"
#include "columnadapter.h"
#include "enumcolumnadapter.h"
#include "numbercolumnadapter.h"
#include "formats.h"
#include "relationcolumnadapter.h"
#include "../../service/model/accounttype.h"

AccountTableModel::AccountTableModel(QObject *parent) : companies_(QList<Company*>()),
    PodTableModel<Account>{
        QList<ColumnAdapter<Account>*>{
            new BoolColumnAdapter<Account>(tr("Closed"), &Account::closed),
            new RelationColumnAdapter<Account, Company>(tr("Company"), &Account::companyId, &this->companies_),
            new ColumnAdapter<Account>(tr("Name"), &Account::name),
            new EnumColumnAdapter<Account, AccountType>(tr("Type"), &Account::type, accountTypes),
            new ColumnAdapter<Account>(tr("Description"), &Account::description),
            new ColumnAdapter<Account>(tr("Number"), &Account::accountNumber),
            new NumberColumnAdapter<Account>(tr("Transactions"), &Account::transactions),
            new AmountColumnAdapter<Account>(tr("Balance"), &Account::balance, accountBalance),
        },
        parent,
    }
{}

const QList<Company*> AccountTableModel::companies() const {
    return companies_;
}

void AccountTableModel::setCompanies(QList<Company*> companies) {
    this->beginResetModel();
    this->companies_.clear();
    this->companies_.append(companies);
    this->endResetModel();
}
