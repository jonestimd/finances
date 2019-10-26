// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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
package io.github.jonestimd.finance.domain.fileimport;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "import_page_region",
       uniqueConstraints = { @UniqueConstraint(name = "import_page_region_ak", columnNames = {"import_file_id", "name"}) })
@SequenceGenerator(name = "id_generator", sequenceName = "import_page_region_id_seq")
public class PageRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "name", nullable = false, length = 250)
    private String name;
    @Column(name = "top")
    private Float top;
    @Column(name = "bottom")
    private Float bottom;
    @Column(name = "label_left")
    private Float labelLeft;
    @Column(name = "label_right")
    private Float labelRight;
    @Column(name = "value_left")
    private Float valueLeft;
    @Column(name = "value_right")
    private Float valueRight;

    public PageRegion() {}

    public PageRegion(String name, Float top, Float bottom, Float labelLeft, Float labelRight, Float valueLeft, Float valueRight) {
        this.name = name;
        this.top = top;
        this.bottom = bottom;
        this.labelLeft = labelLeft;
        this.labelRight = labelRight;
        this.valueLeft = valueLeft;
        this.valueRight = valueRight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getTop() {
        return top == null ? Float.POSITIVE_INFINITY : top;
    }

    public void setTop(Float top) {
        this.top = top;
    }

    public float getBottom() {
        return bottom == null ? Float.NEGATIVE_INFINITY : bottom;
    }

    public void setBottom(Float bottom) {
        this.bottom = bottom;
    }

    public float getLabelLeft() {
        return labelLeft == null ? Float.NEGATIVE_INFINITY : labelLeft;
    }

    public void setLabelLeft(Float labelLeft) {
        this.labelLeft = labelLeft;
    }

    public float getLabelRight() {
        return labelRight == null ? Float.POSITIVE_INFINITY : labelRight;
    }

    public void setLabelRight(Float labelRight) {
        this.labelRight = labelRight;
    }

    public float getValueLeft() {
        return valueLeft == null ? Float.NEGATIVE_INFINITY : valueLeft;
    }

    public void setValueLeft(Float valueLeft) {
        this.valueLeft = valueLeft;
    }

    public float getValueRight() {
        return valueRight == null ? Float.POSITIVE_INFINITY : valueRight;
    }

    public void setValueRight(Float valueRight) {
        this.valueRight = valueRight;
    }
}
