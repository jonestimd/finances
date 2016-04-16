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
package io.github.jonestimd.finance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Initializes the classpath and launches a main class based on the following settings in {@code launcher.properties}.
 * <ul>
 *     <li><strong>classpath.append</strong> - new line separated list of directories to append to the classpath</li>
 *     <ul style="list-style-type: circle">
 *         <li>entries ending in {@code /*} are searched for jars to include in the classpath</li>
 *         <li>otherwise, the specified directory is included in the classpath</li>
 *         <li>placeholders of the form {@code ${property}} are replaced with the value of the system property
 *         or the property from the configuration file if the system property is not set</li>
 *         <li> the {@code install.dir} system property is set to the directory from which the application was launched</li>
 *     </ul>
 *     <li><strong>main.default</strong> - the name of the default {@code main} class</li>
 *     <li><strong>main.<em>alias</em></strong> - alternate {@code main} classes that can be selected using the {@code --run} command line option</li>
 * </ul>
 * If the first command line argument is {@code --run} then the second argument specifies the {@code alias} of one of the
 * {@code main} entries in the configuration file.  Otherwise, the {@code main.default} entry is used.  The {@code main()}
 * method of the selected class is invoked with the command line arguments, excluding the {@code --run} option.
 * <p/>
 * Below is a sample configuration file with one alternate {@code main} and 2 driver directories for additional jars.
 * The last driver directory can be overridden on the command line using {@code -Ddrivers=path}.
 * <pre>
 * main.default=com.example.Application
 * main.util=com.example.Utility
 * drivers=${install.dir}/drivers
 * classpath.append=\
 *   ${user.home}/.app/drivers/*\n\
 *   ${drivers}/*
 * </pre>
 */
public class ApplicationLauncher {
    private final List<URL> pluginUrls = new LinkedList<>();
    private final Properties config = new Properties();

    public static void main(String[] args) {
        try (final InputStream is = ApplicationLauncher.class.getResourceAsStream("/java-logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (Exception e) {
            System.err.println("error reading java-logging.properties");
        }
        try {
            new ApplicationLauncher().launch(args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ApplicationLauncher() throws URISyntaxException, IOException {
        System.setProperty("install.dir", getInstallDirectory());
        config.load(getClass().getResourceAsStream("/launcher.properties"));
        String extensions = config.getProperty("classpath.append");
        if (extensions != null) {
            for (String dir : extensions.split("\n")) {
                addDirectory(resolvePlaceholders(dir));
            }
        }
    }

    private String getInstallDirectory() throws URISyntaxException {
        File source = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        return source.isFile() ? source.getParent() : source.getPath();
    }

    private void addDirectory(String path) throws URISyntaxException, MalformedURLException {
        if (path.endsWith("/*")) {
            addPaths(new File(path.substring(0, path.length() - 2)));
        }
        else {
            pluginUrls.add(new File(path).toURI().toURL());
        }
    }

    private String resolvePlaceholders(String path) {
        Pattern pattern = Pattern.compile("(\\$\\{[^}]+\\})");
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = pattern.matcher(path);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, getValue(matcher.group()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String getValue(String placeholder) {
        String property = placeholder.substring(2, placeholder.length()-1);
        String value = System.getProperty(property);
        return value != null ? value : resolvePlaceholders(config.getProperty(property));
    }

    private void addPaths(File parent) {
        if (parent.exists() && parent.isDirectory()) {
            for (File file : parent.listFiles()) {
                if (file.isDirectory() || file.getName().toLowerCase().endsWith(".jar")) {
                    try {
                        pluginUrls.add(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        System.err.println("ignoring classpath file: " + file.toString());
                    }
                }
            }
        }
    }

    private void launch(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        URLClassLoader classLoader = createClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        String selector = (args.length > 1 && args[0].equals("--run")) ? args[1] : "default";
        String mainClassName = config.getProperty("main." + selector);
        if (mainClassName != null) {
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
        }
        else {
            System.err.println("Invalid option for --run: " + selector);
        }
    }

    private URLClassLoader createClassLoader() {
        return new URLClassLoader(pluginUrls.toArray(new URL[pluginUrls.size()]), getClass().getClassLoader());
    }
}
