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

    /*!
     * \brief getId Get the id of a \c NamedEntity\c.
     * \param value the \c NamedEntity\c
     */
    static QVariant getId(const QVariant &value);
    static QString getName(const NamedEntity *entity);
};

Q_DECLARE_METATYPE(const NamedEntity*)
// static const int namedEntityTypeId = qRegisterMetaType<NamedEntity>();

struct EnumValue : QObject {
    const char *code;
    const QString name;

    EnumValue(const char *code, const QString name);

    static bool less(const EnumValue *, const EnumValue *);
};

Q_DECLARE_METATYPE(const EnumValue*)

template<typename T>
concept NameAndId = std::is_base_of<NamedEntity, T>::value;

template<NameAndId T>
using ValuesSupplier = std::function<const QHash<qlonglong, const T*>()>;

#endif // BASEDOMAIN_H
