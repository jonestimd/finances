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
package io.github.jonestimd.finance.swing;

import java.awt.Component;
import java.util.function.Predicate;

import javax.swing.JButton;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.junit.After;
import org.junit.Before;

public class SwingRobotTest {
    protected Robot robot;

    @Before
    public void setUp() throws Exception {
        robot = BasicRobot.robotWithCurrentAwtHierarchy();
    }

    @After
    public void cleanUp() throws Exception {
        robot.cleanUp();
    }

    protected GenericTypeMatcher<JButton> buttonMatcher(String text) {
        return matcher(JButton.class, button -> button.getText().equals(text));
    }

    protected  <T extends Component> GenericTypeMatcher<T> matcher(Class<T> componentType, Predicate<T> condition) {
        return new GenericTypeMatcher<T>(componentType) {
            @Override
            protected boolean isMatching(T component) {
                return condition.test(component);
            }
        };
    }
}
