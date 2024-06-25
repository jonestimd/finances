#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "../../service/model/account.h"
#include "datastore.h"
#include "podtablemodel.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account> {
    DataStore *dataStore;
public:
    explicit AccountTableModel(DataStore *datastore, QObject *parent = nullptr);
};

#endif // ACCOUNTTABLEMODEL_H
