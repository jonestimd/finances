#include "payeeservice.h"
#include "service/database/transactiondao.h"

PayeeService::PayeeService(ConnectionPool *connectionPool, PayeeDao &payeeDao, TransactionDao &transactionDao)
    : NamedEntityService{connectionPool, payeeDao}
    , transactionDao{transactionDao}
{}

QList<const Payee*> PayeeService::merge(const Payee *payee, domain_id destinationId, const QString &user) {
    return doInTransaction<QList<const Payee*>>([=, this](QSqlDatabase &db) {
        transactionDao.replacePayee(db, payee, destinationId, user);
        dao.remove(db, QList{payee});
        return dao.get(db, QList{destinationId});
    });
}
