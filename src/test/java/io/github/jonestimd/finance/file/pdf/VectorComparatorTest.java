package io.github.jonestimd.finance.file.pdf;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.pdfbox.util.Vector;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class VectorComparatorTest {
    @Test
    public void topDown_sortsByDescendingY() throws Exception {
        List<Vector> vectors = Lists.newArrayList(
                new Vector(3f, 6f),
                new Vector(1f, 9f),
                new Vector(2f, 8f));

        vectors.sort(VectorComparator.TOP_DOWN);

        assertThat(vectors.stream().map(Vector::getY)).containsExactly(9f, 8f, 6f);
    }

    @Test
    public void leftToRight_sortsByAscendingX() throws Exception {
        List<Vector> vectors = Lists.newArrayList(
                new Vector(3f, 6f),
                new Vector(1f, 9f),
                new Vector(2f, 8f));

        vectors.sort(VectorComparator.LEFT_TO_RIGHT);

        assertThat(vectors.stream().map(Vector::getX)).containsExactly(1f, 2f, 3f);
    }

    @Test
    public void topDownLeftToRight_sortsByDescendingYThenAscendingX() throws Exception {
        List<Vector> vectors = Lists.newArrayList(
                new Vector(5f, 6f),
                new Vector(3f, 6f),
                new Vector(1f, 9f),
                new Vector(11f, 9f),
                new Vector(7f, 8f),
                new Vector(2f, 8f));

        vectors.sort(VectorComparator.TOP_DOWN_LEFT_TO_RIGHT);

        assertThat(vectors.stream().map(Vector::getY)).containsExactly(9f, 9f, 8f, 8f, 6f, 6f);
        assertThat(vectors.stream().map(Vector::getX)).containsExactly(1f, 11f, 2f, 7f, 3f, 5f);
    }
}