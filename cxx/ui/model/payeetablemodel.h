#ifndef PAYEETABLEMODEL_H
#define PAYEETABLEMODEL_H

#include "datastore.h"
#include "service/model/payee.h"
#include "podtablemodel.h"

class PayeeTableModel : public PodTableModel<Payee> {
public:
    explicit PayeeTableModel(DataStore *dataStore, QObject *parent = nullptr);
};

#endif // PAYEETABLEMODEL_H
