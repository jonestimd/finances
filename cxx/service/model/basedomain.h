#ifndef BASEDOMAIN_H
#define BASEDOMAIN_H

#include <QSqlRecord>
#include <QVariant>

class BaseDomain {
public:
    QVariant id;
    QVariant version{0};
    QVariant changeUser;
    QVariant changeDate;

    BaseDomain();
    BaseDomain(const QSqlRecord &record);
};

class NamedEntity : public BaseDomain {
public:
    QVariant name;

    NamedEntity() = default;
    NamedEntity(const QSqlRecord &record, const char *nameColumn = "name");
    NamedEntity(const QString &name);

    static QString getName(const NamedEntity *entity);
};

Q_DECLARE_METATYPE(const NamedEntity*)
// static const int namedEntityTypeId = qRegisterMetaType<NamedEntity>();

class Category;

class TransactionType : public NamedEntity {
public:
    const bool transfer;

    TransactionType(bool transfer);
    TransactionType(bool transfer, const QSqlRecord &record, const char *nameColumn = "name");

    static const TransactionType *get(const QVariant &value);
    static const Category *getCategory(const QVariant &value);
};

Q_DECLARE_METATYPE(const TransactionType*)

struct TransactionTypeId {
    const bool transfer;
    const QVariant id;

    TransactionTypeId(bool transfer = false, QVariant id = QVariant{});
    TransactionTypeId(const TransactionType &tt);
    TransactionTypeId(const TransactionType *tt);
};

Q_DECLARE_METATYPE(const TransactionTypeId)

struct EnumValue : QObject {
    const char *code;
    const QString name;

    EnumValue(const char *code, const QString name);

    static bool less(const EnumValue *, const EnumValue *);
};

Q_DECLARE_METATYPE(const EnumValue*)

template<typename T>
concept NameAndId = std::is_base_of<NamedEntity, T>::value;

#endif // BASEDOMAIN_H
