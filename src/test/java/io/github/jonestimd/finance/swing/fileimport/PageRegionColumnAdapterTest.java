package io.github.jonestimd.finance.swing.fileimport;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import org.junit.Test;

import static io.github.jonestimd.finance.swing.fileimport.PageRegionColumnAdapter.*;
import static org.assertj.core.api.Assertions.*;

public class PageRegionColumnAdapterTest {
    private PageRegion newPageRegion(String name) {
        return new PageRegion(name, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    @Test
    public void nameIsRequired() throws Exception {
        String message = NAME_ADAPTER.validate(0, "", new ArrayList<>());

        assertThat(message).isEqualTo("Name is required");
    }

    @Test
    public void nameMustBeUnique() throws Exception {
        String message = NAME_ADAPTER.validate(0, "two", Arrays.asList(newPageRegion("one"), newPageRegion("two")));

        assertThat(message).isEqualTo("Name must be unique");
    }

    @Test
    public void nameValidatorIgnoresSelectedRow() throws Exception {
        String message = NAME_ADAPTER.validate(0, "one", Arrays.asList(newPageRegion("one"), newPageRegion("two")));

        assertThat(message).isNull();
    }
}