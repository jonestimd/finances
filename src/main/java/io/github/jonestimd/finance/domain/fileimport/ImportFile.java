// The MIT License (MIT)
//
// Copyright (c) 2024 Tim Jones
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

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import com.google.common.collect.ListMultimap;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.file.DomainMapper;
import io.github.jonestimd.finance.file.GroupedDetailImportContext;
import io.github.jonestimd.finance.file.ImportContext;
import io.github.jonestimd.finance.file.MultiDetailImportContext;
import io.github.jonestimd.finance.file.SingleDetailImportContext;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import static io.github.jonestimd.finance.domain.fileimport.ImportCategory.*;

@Entity
@Table(name = "import_file", uniqueConstraints = {@UniqueConstraint(name = "import_file_ak", columnNames = {"name"})})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "id_generator", sequenceName = "import_file_id_seq")
@NamedQuery(name = ImportFile.FIND_ONE_BY_NAME, query = "from ImportFile where name = :name")
public class ImportFile implements UniqueId<Long>, Cloneable {
    public static final String FIND_ONE_BY_NAME = "ImportFile.findOneByName";
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "name", nullable = false, length = 250)
    private String name;
    @Column(name = "file_type", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "import_file_id", nullable = false, foreignKey = @ForeignKey(name = "import_field_import_file_fk"))
    private Set<ImportField> fields;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "import_file_id", nullable = false, foreignKey = @ForeignKey(name = "import_page_region_import_file_fk"))
    private Set<PageRegion> pageRegions;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "import_category",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_category_file_fk"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"import_file_id", "type_alias"}))
    private Set<ImportCategory> importCategories;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "import_transfer_account",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_tx_account_file_fk"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"import_file_id", "account_alias"}))
    private Set<ImportTransfer> importTransfers;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "import_payee",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_payee_file_fk"),
            inverseJoinColumns = @JoinColumn(name = "payee_id", nullable = false, unique = false),
            inverseForeignKey = @ForeignKey(name = "import_payee_payee_fk"))
    @MapKeyColumn(name = "payee_alias")
    private Map<String, Payee> payeeMap;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "import_security",
            joinColumns = @JoinColumn(name = "import_file_id", nullable = false),
            foreignKey = @ForeignKey(name = "import_security_file_fk"),
            inverseJoinColumns = @JoinColumn(name = "security_id", nullable = false, unique = false),
            inverseForeignKey = @ForeignKey(name = "import_security_security_fk"))
    @MapKeyColumn(name = "security_alias")
    private Map<String, Security> securityMap;
    @ManyToOne
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "import_file_account_fk"))
    private Account account;
    @ManyToOne
    @JoinColumn(name = "payee_id", foreignKey = @ForeignKey(name = "import_file_payee_fk"))
    private Payee payee;
    @Column(name = "import_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportType importType;
    @Column(name = "start_offset", nullable = false)
    private int startOffset;
    @Column(name = "reconcile", nullable = false)
    @Type(type = "yes_no")
    private boolean reconcile;
    @Column(name = "date_format", length = 50, nullable = false)
    private String dateFormat;

    public ImportFile() {}

    public ImportFile(String name, ImportType importType, FileType fileType, String dateFormat) {
        this.name = name;
        this.fileType = fileType;
        this.importType = importType;
        this.dateFormat = dateFormat;
    }

    public Iterable<ListMultimap<ImportField, String>> parse(InputStream stream) throws Exception {
        return fileType.parse(this, stream);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public Set<ImportField> getFields() {
        return fields;
    }

    public void setFields(Set<ImportField> fields) {
        this.fields = fields;
    }

    public Set<PageRegion> getPageRegions() {
        return pageRegions;
    }

    public void setPageRegions(Set<PageRegion> pageRegions) {
        this.pageRegions = pageRegions;
    }

    public Set<ImportCategory> getImportCategories() {
        return importCategories;
    }

    public void setImportCategories(Set<ImportCategory> importCategories) {
        this.importCategories = importCategories;
    }

    public Map<String, TransactionCategory> getCategoryMap() {
        return importCategories.stream().collect(Collectors.toMap(ImportCategory::getAlias, ImportCategory::getCategory));
    }

    public boolean isNegate(TransactionCategory category) {
        return category != null && importCategories.stream().filter(c -> c.getCategory().equals(category))
                .findFirst().orElse(EMPTY_IMPORT_CATEGORY).isNegate();
    }

    public Set<ImportTransfer> getImportTransfers() {
        return importTransfers;
    }

    public void setImportTransfers(Set<ImportTransfer> importTransfers) {
        this.importTransfers = importTransfers;
    }

    public Account getTransferAccount(String key) {
        ImportTransfer transfer = importTransfers.stream().filter(t -> t.getAlias().equals(key)).findFirst().orElse(null);
        return transfer == null ? null : transfer.getAccount();
    }

    public Map<String, Payee> getPayeeMap() {
        return payeeMap;
    }

    public void setPayeeMap(Map<String, Payee> payeeMap) {
        this.payeeMap = payeeMap;
    }

    public Map<String, Security> getSecurityMap() {
        return securityMap;
    }

    public void setSecurityMap(Map<String, Security> securityMap) {
        this.securityMap = securityMap;
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

    public ImportType getImportType() {
        return importType;
    }

    public void setImportType(ImportType importType) {
        this.importType = importType;
    }

    public boolean isReconcile() {
        return reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }

    public Integer getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Integer startOffset) {
        this.startOffset = startOffset;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat(dateFormat).parse(dateString);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ImportContext newContext(Collection<Payee> payees, Collection<Security> securities, Collection<TransactionCategory> categories) {
        return newContext(new DomainMapper<>(payees, Payee::getName, payeeMap, Payee::new),
                new DomainMapper<>(securities, Security::getName, securityMap, name -> new Security(name, SecurityType.STOCK)),
                new DomainMapper<>(categories, TransactionCategory::getCode, getCategoryMap(), null));
    }

    public ImportContext newContext(DomainMapper<Payee> payeeMapper, DomainMapper<Security> securityMapper, DomainMapper<TransactionCategory> categoryMapper) {
        if (importType == ImportType.MULTI_DETAIL_ROWS) {
            return new MultiDetailImportContext(this, payeeMapper, securityMapper);
        }
        if (importType == ImportType.SINGLE_DETAIL_ROWS) {
            new SingleDetailImportContext(this, payeeMapper, securityMapper, categoryMapper);
        }
        return new GroupedDetailImportContext(this, payeeMapper, securityMapper, categoryMapper);
    }

    public ImportFile clone() {
        try {
            ImportFile clone = (ImportFile) super.clone();
            clone.id = null;
            Map<PageRegion, PageRegion> regionMap = pageRegions.stream().collect(Collectors.toMap(Function.identity(), PageRegion::clone));
            clone.pageRegions = new HashSet<>(regionMap.values());
            clone.fields = new HashSet<>();
            for (ImportField field : fields) {
                ImportField cloneField = field.clone();
                if (cloneField.getRegion() != null) cloneField.setRegion(regionMap.get(cloneField.getRegion()));
                clone.fields.add(cloneField);
            }
            clone.importTransfers = importTransfers.stream().map(ImportTransfer::clone).collect(Collectors.toSet());
            clone.importCategories = importCategories.stream().map(ImportCategory::clone).collect(Collectors.toSet());
            clone.payeeMap = new HashMap<>(payeeMap);
            clone.securityMap = new HashMap<>(securityMap);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static ImportFile newImport() {
        ImportFile importFile = new ImportFile();
        importFile.setName("");
        importFile.setDateFormat("");
        importFile.setFields(new HashSet<>());
        importFile.setPageRegions(new HashSet<>());
        importFile.setImportCategories(new HashSet<>());
        importFile.setImportTransfers(new HashSet<>());
        importFile.setPayeeMap(new HashMap<>());
        importFile.setSecurityMap(new HashMap<>());
        return importFile;
    }
}
