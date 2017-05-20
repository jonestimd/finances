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

import java.util.function.Consumer;

import javax.swing.UIManager;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.log4j.Logger;

import static io.github.jonestimd.finance.config.ApplicationConfig.*;

public class LookAndFeelConfig {
    private static final String LOOK_AND_FEEL = "finances.lookAndFeel";
    private static final String OPTIONS = LOOK_AND_FEEL + ".options";
    private static final String LOADER = "loader";
    private static final Logger logger = Logger.getLogger(LookAndFeelConfig.class);

    @SuppressWarnings("unchecked")
    public static void load() {
        try {
            String lafClass = CONFIG.getString(LOOK_AND_FEEL + ".class");
            Config options = getOptions(lafClass);
            if (options.hasPath(LOADER)) {
                Consumer<Config> configurer = Consumer.class.cast(Class.forName(options.getString(LOADER)).newInstance());
                configurer.accept(options.withoutPath("loader"));
            }
            UIManager.setLookAndFeel(lafClass);
            UIManager.getDefaults().addResourceBundle(UiOverrideBundle.class.getName());
        }
        catch (Exception ex) {
            logger.warn("Failed to set look and feel", ex);
        }
    }

    private static Config getOptions(String lafClass) {
        String lafOptions = lafClass.replaceFirst(".*\\.", LOOK_AND_FEEL + ".") + ".options";
        if (CONFIG.hasPath(OPTIONS)) return CONFIG.getConfig(OPTIONS);
        if (CONFIG.hasPath(lafOptions)) return CONFIG.getConfig(lafOptions);
        return ConfigFactory.empty();
    }
}
