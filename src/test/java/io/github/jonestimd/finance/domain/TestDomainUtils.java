package io.github.jonestimd.finance.domain;

import java.lang.reflect.Field;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;

public class TestDomainUtils {

    public static Account createAccount(String name, AccountType type) {
        Account account = create(Account.class);
        account.setName(name);
        account.setType(type);
        return account;
    }

    public static Account createAccount(String name) {
        return createAccount(name, null);
    }

    public static TransactionGroup createTransactionGroup(String name) {
        TransactionGroup group = create(TransactionGroup.class);
        group.setName(name);
        return group;
    }

    public static Payee createPayee(long id, String name) {
        Payee payee = create(Payee.class, id);
        payee.setName(name);
        return payee;
    }

    public static <T> T create(Class<T> domainClass) {
        return create(domainClass, TestSequence.nextId());
    }

    public static <T> T create(Class<T> domainClass, Long id) {
        try {
            T instance = domainClass.newInstance();
            getIdField(domainClass).set(instance, id);
            return instance;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <T> Field getIdField(Class<T> domainClass) {
        return getField(domainClass, UniqueId.ID);
    }

    private static <T> Field getField(Class<T> domainClass, String fieldName) {
        Class<?> clazz = domainClass;
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ex) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new IllegalArgumentException("no id field found on " + domainClass.getName());
    }

    public static <T extends UniqueId<Long>> T setId(T domainObject) {
        return setId(domainObject, TestSequence.nextId());
    }

    public static <T extends UniqueId<K>, K> T setId(T domainObject, K id) {
        try {
            getIdField(domainObject.getClass()).set(domainObject, id);
            return domainObject;
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}