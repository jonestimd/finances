package io.github.jonestimd.collection;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ReferenceIteratorTest {
    @Test
    public void testIteratorReturnsAllItems() throws Exception {
        List<WeakReference<String>> references = new ArrayList<WeakReference<String>>();
        references.add(new WeakReference<String>("One"));
        references.add(new WeakReference<String>("Two"));
        references.add(new WeakReference<String>("Three"));

        ReferenceIterator<String> iterator = ReferenceIterator.iterator(references);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("One");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("Two");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("Three");
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void testIteratorRemovesClearedReferences() throws Exception {
        List<WeakReference<String>> references = new ArrayList<WeakReference<String>>();
        references.add(new WeakReference<String>("One"));
        references.add(new WeakReference<String>("Two"));
        references.add(new WeakReference<String>("Three"));
        references.get(1).clear();

        ReferenceIterator<String> iterator = ReferenceIterator.iterator(references);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("One");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("Three");
        assertThat(iterator.hasNext()).isFalse();
        assertThat(references).hasSize(2);
    }

    @Test
    public void testIteratorDelegatesRemove() throws Exception {
        List<WeakReference<String>> references = new ArrayList<WeakReference<String>>();
        references.add(new WeakReference<String>("One"));
        references.add(new WeakReference<String>("Two"));
        references.add(new WeakReference<String>("Three"));
        references.get(1).clear();

        ReferenceIterator<String> iterator = ReferenceIterator.iterator(references);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("One");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("Three");
        iterator.remove();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(references).hasSize(1);
        assertThat(references.get(0).get()).isEqualTo("One");
    }
}