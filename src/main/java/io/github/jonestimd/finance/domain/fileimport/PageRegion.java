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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "import_page_region")
@SequenceGenerator(name = "id_generator", sequenceName = "import_page_region_id_seq")
public class PageRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @Column(name = "id", nullable = false)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "import_file_id") @ForeignKey(name = "import_page_region_file_fk")
    private ImportFile importFile;
    @Column(name = "top", nullable = false)
    private Float top;
    @Column(name = "bottom", nullable = false)
    private Float bottom;
    @Column(name = "label_left", nullable = false)
    private Float labelLeft;
    @Column(name = "label_right", nullable = false)
    private Float labelRight;
    @Column(name = "value_left", nullable = false)
    private Float valueLeft;
    @Column(name = "value_right", nullable = false)
    private Float valueRight;

    public PageRegion() {}

    public PageRegion(Float top, Float bottom, Float labelLeft, Float labelRight, Float valueLeft, Float valueRight) {
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

    public ImportFile getImportFile() {
        return importFile;
    }

    public void setImportFile(ImportFile importFile) {
        this.importFile = importFile;
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
