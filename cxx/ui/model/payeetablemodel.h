#ifndef PAYEETABLEMODEL_H
#define PAYEETABLEMODEL_H

#include "service/model/payee.h"
#include "podtablemodel.h"
#include "ui/store/payeestore.h"

class PayeeTableModel : public PodTableModel<Payee, PayeeStore> {
public:
    explicit PayeeTableModel(PayeeStore *payeeStore);
};

#endif // PAYEETABLEMODEL_H
