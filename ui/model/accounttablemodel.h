#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "../../database/model/account.h"
#include "../../database/model/company.h"
#include "podtablemodel.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account> {
public:
    explicit AccountTableModel(QList<Company*> companies, QList<Account*> accounts, QObject *parent = nullptr);
};

#endif // ACCOUNTTABLEMODEL_H
