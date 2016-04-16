package io.github.jonestimd.collection;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReferenceIteratorTest {
    @Test
    public void testIteratorReturnsAllItems() throws Exception {
        List<WeakReference<String>> references = new ArrayList<WeakReference<String>>();
        references.add(new WeakReference<String>("One"));
        references.add(new WeakReference<String>("Two"));
        references.add(new WeakReference<String>("Three"));

        ReferenceIterator<String> iterator = ReferenceIterator.iterator(references);
        assertTrue(iterator.hasNext());
        assertEquals("One", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("Two", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("Three", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorRemovesClearedReferences() throws Exception {
        List<WeakReference<String>> references = new ArrayList<WeakReference<String>>();
        references.add(new WeakReference<String>("One"));
        references.add(new WeakReference<String>("Two"));
        references.add(new WeakReference<String>("Three"));
        references.get(1).clear();

        ReferenceIterator<String> iterator = ReferenceIterator.iterator(references);
        assertTrue(iterator.hasNext());
        assertEquals("One", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("Three", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(2, references.size());
    }

    @Test
    public void testIteratorDelegatesRemove() throws Exception {
        List<WeakReference<String>> references = new ArrayList<WeakReference<String>>();
        references.add(new WeakReference<String>("One"));
        references.add(new WeakReference<String>("Two"));
        references.add(new WeakReference<String>("Three"));
        references.get(1).clear();

        ReferenceIterator<String> iterator = ReferenceIterator.iterator(references);
        assertTrue(iterator.hasNext());
        assertEquals("One", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("Three", iterator.next());
        iterator.remove();
        assertFalse(iterator.hasNext());
        assertEquals(1, references.size());
        assertEquals("One", references.get(0).get());
    }
}