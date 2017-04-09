package io.github.jonestimd.reflect;

import java.util.ArrayList;
import java.util.List;

import io.github.jonestimd.util.JavaPredicates;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class PackageScannerTest {
    private List<Class<?>> classes = new ArrayList<Class<?>>();

    @Test
    public void testVisitClassesFromJar() throws Exception {
        new PackageScanner(JavaPredicates.alwaysTrue(), classes::add, "io.github.jonestimd.beans").visitClasses();

        assertThat(classes.isEmpty()).isFalse();
        for (Class<?> match : classes) {
            assertThat(match.getName().startsWith("io.github.jonestimd.beans.")).isTrue();
        }
    }

    @Test
    public void testVisitClassesFromFiles() throws Exception {
        new PackageScanner(JavaPredicates.alwaysTrue(), classes::add, "io.github.jonestimd.collection").visitClasses();

        assertThat(classes.isEmpty()).isFalse();
        for (Class<?> match : classes) {
            assertThat(match.getName().startsWith("io.github.jonestimd.collection.")).isTrue();
        }
    }
}