// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.domain;

import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UniqueIdTest {
    @Mock
    private UniqueId<Long> id1;
    @Mock
    private UniqueId<Long> id2;

    @Test
    public void isSameIdReturnsFalseForNulls() throws Exception {
        assertThat(UniqueId.isSameId(null, null)).isFalse();
        assertThat(UniqueId.isSameId(id1, null)).isFalse();
        assertThat(UniqueId.isSameId(null, id2)).isFalse();

        Mockito.verifyZeroInteractions(id1, id2);
    }

    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void isSameIdReturnsTrue() throws Exception {
        long id = new Random().nextLong();
        when(id1.getId()).thenReturn(new Long(id));
        when(id2.getId()).thenReturn(new Long(id));

        assertThat(UniqueId.isSameId(id1, id2)).isTrue();
        assertThat(UniqueId.isSameId(id2, id1)).isTrue();
    }

    @Test
    public void isSameIdReturnsFalse() throws Exception {
        long id = new Random().nextLong();
        when(id1.getId()).thenReturn(id);
        when(id2.getId()).thenReturn(id + 1);

        assertThat(UniqueId.isSameId(id1, id2)).isFalse();
        assertThat(UniqueId.isSameId(id2, id1)).isFalse();
    }
}