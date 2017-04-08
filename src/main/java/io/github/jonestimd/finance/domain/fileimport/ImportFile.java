// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.domain.fileimport;

import java.util.Collection;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.file.DomainMapper;
import io.github.jonestimd.finance.file.FieldValueExtractor;
import io.github.jonestimd.finance.file.ImportContext;
import io.github.jonestimd.finance.file.MultiDetailImportContext;
import io.github.jonestimd.finance.file.SingleDetailImportContext;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "import_file", uniqueConstraints = {@UniqueConstraint(name = "import_file_ak", columnNames = {"name"})})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "import_type", length = 10)
@SequenceGenerator(name = "id_generator", sequenceName = "import_file_id_seq")
@NamedQuery(name = ImportFile.FIND_ONE_BY_NAME, query = "from ImportFile where name = ?")
public abstract class ImportFile {
    public static final String FIND_ONE_BY_NAME = "ImportFile.findOneByName";
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "name", nullable = false, length = 250)
    private String name;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "import_file_id", nullable = false, foreignKey = @ForeignKey(name = "import_field_import_file_fk"))
    @MapKeyColumn(name = "label", nullable = false, length = 250)
    @org.hibernate.annotations.ForeignKey(name = "import_field_import_file_fk")
    private Map<String, ImportField> fields;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "import_category",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_category_file_fk"),
            inverseJoinColumns = @JoinColumn(name = "tx_category_id", nullable = false),
            inverseForeignKey = @ForeignKey(name = "import_category_category_fk"))
    @MapKeyColumn(name = "type_alias")
    @org.hibernate.annotations.ForeignKey(name = "import_category_file_fk", inverseName = "import_category_category_fk")
    private Map<String, TransactionCategory> categoryMap;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "import_transfer_account",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_tx_account_file_fk"),
            inverseJoinColumns = @JoinColumn(name = "account_id", nullable = false),
            inverseForeignKey = @ForeignKey(name = "import_tx_account_account_fk"))
    @MapKeyColumn(name = "account_alias")
    @org.hibernate.annotations.ForeignKey(name = "import_tx_account_file_fk", inverseName = "import_tx_account_account_fk")
    private Map<String, Account> transferAccountMap;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "import_payee",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_payee_file_fk"),
            inverseJoinColumns = @JoinColumn(name = "payee_id", nullable = false, unique = false),
            inverseForeignKey = @ForeignKey(name = "import_payee_payee_fk"))
    @MapKeyColumn(name = "payee_alias")
    @org.hibernate.annotations.ForeignKey(name = "import_payee_file_fk", inverseName = "import_payee_payee_fk")
    private Map<String, Payee> payeeMap;
    @ManyToOne
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "import_file_account_fk"))
    private Account account;
    @ManyToOne
    @JoinColumn(name = "payee_id", foreignKey = @ForeignKey(name = "import_file_payee_fk"))
    private Payee payee;
    @Column(name = "multi_detail", nullable = false)
    @Type(type = "yes_no")
    private boolean multiDetail;
    @Column(name = "reconcile", nullable = false)
    @Type(type = "yes_no")
    private boolean reconcile;

    protected ImportFile() {
    }

    protected ImportFile(String name) {
        this.name = name;
    }

    public abstract FieldValueExtractor getFieldValueExtractor();

    public abstract String getFileExtension();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, ImportField> getFields() {
        return fields;
    }

    public void setFields(Map<String, ImportField> fields) {
        this.fields = fields;
    }

    public Map<String, TransactionCategory> getCategoryMap() {
        return categoryMap;
    }

    public void setCategoryMap(Map<String, TransactionCategory> categoryMap) {
        this.categoryMap = categoryMap;
    }

    public Account getTransferAccount(String key) {
        return transferAccountMap.get(key);
    }

    public Map<String, Account> getTransferAccountMap() {
        return transferAccountMap;
    }

    public void setTransferAccountMap(Map<String, Account> transferAccountMap) {
        this.transferAccountMap = transferAccountMap;
    }

    public Map<String, Payee> getPayeeMap() {
        return payeeMap;
    }

    public void setPayeeMap(Map<String, Payee> payeeMap) {
        this.payeeMap = payeeMap;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Payee getPayee() {
        return payee;
    }

    public void setPayee(Payee payee) {
        this.payee = payee;
    }

    public boolean isMultiDetail() {
        return multiDetail;
    }

    public void setMultiDetail(boolean multiDetail) {
        this.multiDetail = multiDetail;
    }

    public boolean isReconcile() {
        return reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }

    public ImportContext newContext(Collection<Payee> payees, Collection<TransactionCategory> categories) {
        return newContext(new DomainMapper<>(payees, Payee::getName, payeeMap, Payee::new),
                new DomainMapper<>(categories, TransactionCategory::getCode, categoryMap, null));
    }

    public ImportContext newContext(DomainMapper<Payee> payeeMapper, DomainMapper<TransactionCategory> categoryMapper) {
        return multiDetail ? new MultiDetailImportContext(this, payeeMapper, categoryMapper) :
                new SingleDetailImportContext(this, payeeMapper, categoryMapper);

    }
}
