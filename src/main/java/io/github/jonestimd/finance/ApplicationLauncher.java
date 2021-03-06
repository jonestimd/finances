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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

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
    private final List<URL> classpathUrls = new LinkedList<>();
    private final List<URL> pluginUrls = new LinkedList<>();
    private final Properties config = new Properties();
    private final Logger logger = Logger.getLogger(getClass());

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
        if (! System.getProperties().containsKey("install.dir")) System.setProperty("install.dir", getInstallDirectory());
        config.load(getClass().getResourceAsStream("/launcher.properties"));
        logger.debug("install dir: " + System.getProperty("install.dir"));
        logger.debug("user home: " + System.getProperty("user.home"));
        gatherFiles(config.getProperty("classpath.append"), classpathUrls::add);
        gatherFiles(config.getProperty("extension.classpath"), pluginUrls::add);
    }

    private void gatherFiles(String paths, Consumer<URL> consumer) throws URISyntaxException, MalformedURLException {
        if (paths != null) {
            for (String dir: paths.split("\n")) {
                addDirectory(resolvePlaceholders(dir), consumer);
            }
        }
    }

    private String getInstallDirectory() throws URISyntaxException, UnsupportedEncodingException {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File source = new File(URLDecoder.decode(path, "UTF-8"));
        return source.isFile() ? source.getParent() : source.getPath();
    }

    private void addDirectory(String path, Consumer<URL> consumer) throws URISyntaxException, MalformedURLException {
        logger.debug("adding file(s) " + path);
        if (path.endsWith("/*")) {
            addPaths(new File(path.substring(0, path.length() - 2)), consumer);
        }
        else {
            consumer.accept(new File(path).toURI().toURL());
        }
    }

    private String resolvePlaceholders(String path) {
        Pattern pattern = Pattern.compile("(\\$\\{[^}]+})");
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
        return value != null ? escapePath(value) : resolvePlaceholders(config.getProperty(property));
    }

    private String escapePath(String replacement) {
        return replacement.replaceAll("\\\\", "/").replaceAll("\\$", "\\\\\\$");
    }

    private void addPaths(File parent, Consumer<URL> consumer) {
        if (parent.exists() && parent.isDirectory()) {
            for (File file : parent.listFiles()) {
                if (file.isDirectory() || file.getName().toLowerCase().endsWith(".jar")) {
                    try {
                        consumer.accept(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        System.err.println("ignoring classpath file: " + file.toString());
                    }
                }
            }
        }
    }

    private void launch(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        appendClasspath();
        URLClassLoader classLoader = createClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        String selector = "default";
        if (args.length > 1 && args[0].equals("--run")) {
            selector = args[1];
            args = Arrays.copyOfRange(args, 2, args.length);
        }
        String mainClassName = config.getProperty("main." + selector);
        if (mainClassName != null) {
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
        }
        else {
            System.err.println("Invalid option for --run: " + selector);
        }
    }

    private void appendClasspath() throws NoSuchMethodException {
        logger.debug("classpath files: " + classpathUrls);
        if (!classpathUrls.isEmpty()) {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            Method appendClassPath = URLClassLoader.class.getDeclaredMethod("addURL", URL.class); // TODO doesn't work after Java 8
            appendClassPath.setAccessible(true);
            classpathUrls.forEach(url -> {
                try {
                    appendClassPath.invoke(systemClassLoader, url);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private URLClassLoader createClassLoader() {
        logger.debug("plugin files: " + pluginUrls);
        return new URLClassLoader(pluginUrls.toArray(new URL[pluginUrls.size()]), getClass().getClassLoader());
    }
}
