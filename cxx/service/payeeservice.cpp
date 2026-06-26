#include "payeeservice.h"
#include "service/database/transactiondao.h"

PayeeService::PayeeService(ConnectionPool *connectionPool, PayeeDao &payeeDao, TransactionDao &transactionDao)
    : EntityService{connectionPool, payeeDao}
    , transactionDao{transactionDao}
{}

QHash<domain_id, const Payee*> PayeeService::merge(const Payee *payee, domain_id destinationId, const QString &user) {
    return doInTransaction<QHash<domain_id, const Payee*>>([=, this](QSqlDatabase &db) {
        transactionDao.replacePayee(db, payee, destinationId, user);
        dao.remove(db, QList{payee});
        return dao.get(db, QList{destinationId});
    });
}
