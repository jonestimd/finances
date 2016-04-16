package io.github.jonestimd.reflect;

import java.util.ArrayList;
import java.util.List;

import io.github.jonestimd.util.JavaPredicates;
import org.junit.Test;

import static org.junit.Assert.*;

public class PackageScannerTest {
    private List<Class<?>> classes = new ArrayList<Class<?>>();

    @Test
    public void testVisitClassesFromJar() throws Exception {
        new PackageScanner(JavaPredicates.alwaysTrue(), classes::add, "io.github.jonestimd.beans").visitClasses();

        assertFalse(classes.isEmpty());
        for (Class<?> match : classes) {
            assertTrue(match.getName().startsWith("io.github.jonestimd.beans."));
        }
    }

    @Test
    public void testVisitClassesFromFiles() throws Exception {
        new PackageScanner(JavaPredicates.alwaysTrue(), classes::add, "io.github.jonestimd.collection").visitClasses();

        assertFalse(classes.isEmpty());
        for (Class<?> match : classes) {
            assertTrue(match.getName().startsWith("io.github.jonestimd.collection."));
        }
    }
}