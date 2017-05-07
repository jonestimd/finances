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
package io.github.jonestimd.finance.swing.laf;

import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.apache.log4j.Logger;

import static java.lang.reflect.Modifier.*;

public class JgoodiesPlasticConfig implements Consumer<Config> {
    private static final String LAF_CLASS_NAME = "com.jgoodies.looks.plastic.PlasticLookAndFeel";
    private final Logger logger = Logger.getLogger(getClass());
    private Class<?> lafClass;

    public JgoodiesPlasticConfig() {
        // Fix insets for combo box in a table. Required because ComboBoxCellEditor sets JComboBox.isTableCellEditor to false.
        // May need to apply even if not using JGoodies Plastic Looks
        UIManager.getDefaults().put("ComboBox.editorInsets", new Insets(0, 0, 0, 0));
    }

    @Override
    public void accept(Config config) {
        try {
            lafClass = Class.forName(LAF_CLASS_NAME);
        } catch (Exception ex) {
            logger.error("Error initializing Plastic look and feel", ex);
        }
        for (Entry<String, ConfigValue> entry : config.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    private void set(String propertyName, ConfigValue value) {
        try {
            Optional<Method> setter = getSetter(propertyName);
            if (setter.isPresent()) {
                Class<?> parameterType = setter.get().getParameterTypes()[0];
                if (parameterType.isPrimitive() || parameterType.equals(String.class)) {
                    setter.get().invoke(null, value.unwrapped());
                }
                else setter.get().invoke(null, Class.forName(value.unwrapped().toString()).newInstance());
            }
            else logger.warn("No setter for " + propertyName + " on " + lafClass.getName());
        } catch (Exception ex) {
            logger.error("Error setting " + propertyName + " on " + LAF_CLASS_NAME, ex);
        }
    }

    private Optional<Method> getSetter(String propertyName) {
        String method = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        return Arrays.stream(lafClass.getMethods()).filter(forName(method)).findFirst();
    }

    private Predicate<Method> forName(String name) {
        return method -> method.getName().equals(name) && method.getParameterCount() == 1 && isStatic(method.getModifiers());
    }
}
