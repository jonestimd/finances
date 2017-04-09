package io.github.jonestimd.hibernate;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class MappedClassFilterTest {
    @Test
    public void testClassWithEntityIsTrue() throws Exception {
        assertThat(new MappedClassFilter().test(EntityClass.class)).isTrue();
    }

    @Test
    public void testEntitySubclassIsFalse() throws Exception {
        assertThat(new MappedClassFilter().test(EntitySubclass.class)).isFalse();
    }

    @Test
    public void testClassWithMappedSupperClassIsTrue() throws Exception {
        assertThat(new MappedClassFilter().test(MappedSuper.class)).isTrue();
    }

    @Test
    public void testClassWithoutMappingIsFalse() throws Exception {
        assertThat(new MappedClassFilter().test(Object.class)).isFalse();
    }

    @Entity
    private static class EntityClass {}

    private static class EntitySubclass extends EntityClass {}

    @MappedSuperclass
    private static class MappedSuper {}
}