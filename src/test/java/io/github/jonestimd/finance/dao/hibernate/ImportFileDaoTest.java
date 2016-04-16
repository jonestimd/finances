package io.github.jonestimd.finance.dao.hibernate;

import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.dao.ImportFileDao;
import io.github.jonestimd.finance.dao.TransactionalTestFixture;
import io.github.jonestimd.finance.domain.fileimport.pdf.PdfImportFile;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class ImportFileDaoTest extends TransactionalTestFixture {
    private static final String TEST_PDF_IMPORT = "test PDF import";
    private ImportFileDao importFileDao;

    @Override
    protected List<QueryBatch> getInsertQueries() {
        return Collections.emptyList();
    }

    @Before
    public void setUp() throws Exception {
        importFileDao = daoContext.getImportFileDao();
    }

    @Test
    public void findByIdUsesHibernateDao() throws Exception {
        final String name = TEST_PDF_IMPORT;
        PdfImportFile pdfImport = importFileDao.save(new PdfImportFile(name));

        assertThat(importFileDao.get(pdfImport.getId()).getId()).isEqualTo(pdfImport.getId());
    }

    @Test
    public void findOneByName() throws Exception {
        final String name = TEST_PDF_IMPORT;
        importFileDao.save(new PdfImportFile(name));

        assertThat(importFileDao.findOneByName(name).getName()).isEqualTo(TEST_PDF_IMPORT);
    }
}