package io.github.jonestimd.finance.file.quicken.qif;

import org.junit.Test;

import static org.junit.Assert.*;

public class CategoryParserTest {
    @Test
    public void nullCategory() throws Exception {
        CategoryParser parser = new CategoryParser(null);

        assertFalse(parser.isTransfer());
        assertNull(parser.getCategoryNames());
        assertNull(parser.getGroupName());
        assertNull(parser.getAccountName());
    }

    @Test
    public void emptyCategory() throws Exception {
        CategoryParser parser = new CategoryParser("");

        assertFalse(parser.isTransfer());
        assertNull(parser.getCategoryNames());
        assertNull(parser.getGroupName());
        assertNull(parser.getAccountName());
    }

    @Test
    public void categoryWithoutGroup() throws Exception {
        CategoryParser parser = new CategoryParser("category");

        assertFalse(parser.isTransfer());
        assertArrayEquals(new String[] { "category" }, parser.getCategoryNames());
        assertNull(parser.getGroupName());
        assertNull(parser.getAccountName());
    }

    @Test
    public void categoryWithGroup() throws Exception {
        CategoryParser parser = new CategoryParser("category/group");

        assertFalse(parser.isTransfer());
        assertArrayEquals(new String[] { "category" }, parser.getCategoryNames());
        assertEquals("group", parser.getGroupName());
        assertNull(parser.getAccountName());
    }

    @Test
    public void transferWithoutGroup() throws Exception {
        CategoryParser parser = new CategoryParser("[account]");

        assertTrue(parser.isTransfer());
        assertNull(parser.getCategoryNames());
        assertNull(parser.getGroupName());
        assertEquals("account", parser.getAccountName());
    }

    @Test
    public void transferWithGroup() throws Exception {
        CategoryParser parser = new CategoryParser("[account]/group");

        assertTrue(parser.isTransfer());
        assertNull(parser.getCategoryNames());
        assertEquals("group", parser.getGroupName());
        assertEquals("account", parser.getAccountName());
    }

    @Test
    public void groupWithoutCategory() throws Exception {
        CategoryParser parser = new CategoryParser("/group");

        assertFalse(parser.isTransfer());
        assertNull(parser.getCategoryNames());
        assertEquals("group", parser.getGroupName());
        assertNull(parser.getAccountName());
    }
}