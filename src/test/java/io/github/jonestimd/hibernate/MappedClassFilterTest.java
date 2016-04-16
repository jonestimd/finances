package io.github.jonestimd.hibernate;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

import static org.junit.Assert.*;

public class MappedClassFilterTest {
    @Test
    public void testClassWithEntityIsTrue() throws Exception {
        assertTrue(new MappedClassFilter().test(EntityClass.class));
    }

    @Test
    public void testEntitySubclassIsFalse() throws Exception {
        assertFalse(new MappedClassFilter().test(EntitySubclass.class));
    }

    @Test
    public void testClassWithMappedSupperClassIsTrue() throws Exception {
        assertTrue(new MappedClassFilter().test(MappedSuper.class));
    }

    @Test
    public void testClassWithoutMappingIsFalse() throws Exception {
        assertFalse(new MappedClassFilter().test(Object.class));
    }

    @Entity
    private static class EntityClass {}

    private static class EntitySubclass extends EntityClass {}

    @MappedSuperclass
    private static class MappedSuper {}
}