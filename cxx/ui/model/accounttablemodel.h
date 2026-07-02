#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "podtablemodel.h"
#include "comboboxmodel.h"
#include "accountstore.h"
#include "service/model/account.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account, AccountStore> {
    Q_OBJECT
    using AddCompany = ComboBoxModel::CreateValue;

public:
    explicit AccountTableModel(AccountStore *store, AddCompany addCompany = nullptr);

    void companiesLoaded(const QList<domain_id> companyIds);
};

#endif // ACCOUNTTABLEMODEL_H
