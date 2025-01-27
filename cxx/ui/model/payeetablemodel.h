#ifndef PAYEETABLEMODEL_H
#define PAYEETABLEMODEL_H

#include "service/model/payee.h"
#include "payeestore.h"
#include "podtablemodel.h"

class PayeeTableModel : public PodTableModel<Payee, PayeeService> {
public:
    explicit PayeeTableModel(PayeeStore *payeeStore);
};

#endif // PAYEETABLEMODEL_H
