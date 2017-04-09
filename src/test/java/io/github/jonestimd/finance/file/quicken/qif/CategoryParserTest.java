package io.github.jonestimd.finance.file.quicken.qif;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CategoryParserTest {
    @Test
    public void nullCategory() throws Exception {
        CategoryParser parser = new CategoryParser(null);

        assertThat(parser.isTransfer()).isFalse();
        assertThat(parser.getCategoryNames()).isNull();
        assertThat(parser.getGroupName()).isNull();
        assertThat(parser.getAccountName()).isNull();
    }

    @Test
    public void emptyCategory() throws Exception {
        CategoryParser parser = new CategoryParser("");

        assertThat(parser.isTransfer()).isFalse();
        assertThat(parser.getCategoryNames()).isNull();
        assertThat(parser.getGroupName()).isNull();
        assertThat(parser.getAccountName()).isNull();
    }

    @Test
    public void categoryWithoutGroup() throws Exception {
        CategoryParser parser = new CategoryParser("category");

        assertThat(parser.isTransfer()).isFalse();
        assertThat(parser.getCategoryNames()).isEqualTo(new String[] { "category" });
        assertThat(parser.getGroupName()).isNull();
        assertThat(parser.getAccountName()).isNull();
    }

    @Test
    public void categoryWithGroup() throws Exception {
        CategoryParser parser = new CategoryParser("category/group");

        assertThat(parser.isTransfer()).isFalse();
        assertThat(parser.getCategoryNames()).isEqualTo(new String[] { "category" });
        assertThat(parser.getGroupName()).isEqualTo("group");
        assertThat(parser.getAccountName()).isNull();
    }

    @Test
    public void transferWithoutGroup() throws Exception {
        CategoryParser parser = new CategoryParser("[account]");

        assertThat(parser.isTransfer()).isTrue();
        assertThat(parser.getCategoryNames()).isNull();
        assertThat(parser.getGroupName()).isNull();
        assertThat(parser.getAccountName()).isEqualTo("account");
    }

    @Test
    public void transferWithGroup() throws Exception {
        CategoryParser parser = new CategoryParser("[account]/group");

        assertThat(parser.isTransfer()).isTrue();
        assertThat(parser.getCategoryNames()).isNull();
        assertThat(parser.getGroupName()).isEqualTo("group");
        assertThat(parser.getAccountName()).isEqualTo("account");
    }

    @Test
    public void groupWithoutCategory() throws Exception {
        CategoryParser parser = new CategoryParser("/group");

        assertThat(parser.isTransfer()).isFalse();
        assertThat(parser.getCategoryNames()).isNull();
        assertThat(parser.getGroupName()).isEqualTo("group");
        assertThat(parser.getAccountName()).isNull();
    }
}