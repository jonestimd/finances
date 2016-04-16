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
package io.github.jonestimd.reflect;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

public class PackageScanner {
    private Logger logger = Logger.getLogger(PackageScanner.class);
    private Predicate<Class<?>> filter;
    private Consumer<Class<?>> visitor;
    private String[] basePackages;

    public PackageScanner(Predicate<Class<?>> filter, Consumer<Class<?>> visitor, String... basePackages) {
        this.filter = filter;
        this.visitor = visitor;
        this.basePackages = basePackages;
    }

    public void visitClasses() {
        visitClasses(ClassLoader.getSystemClassLoader());
    }

    public void visitClasses(ClassLoader classLoader) {
        try {
            for (String basePackage : basePackages) {
                String filePrefix = getDirectoryName(basePackage);
                Enumeration<URL> resources = classLoader.getResources(filePrefix);
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if ("jar".equals(resource.getProtocol())) {
                        visitJarClasses(filePrefix, resource);
                    } else if ("file".equals(resource.getProtocol())) {
                        visitFileClasses(filePrefix, resource);
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void visitJarClasses(String filePrefix, URL resource) throws IOException {
        JarFile jar = new JarFile(new URL(resource.getFile().substring(0, resource.getFile().indexOf('!'))).getFile());
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            visitClass(filePrefix, entries.nextElement().getName());
        }
    }

    private void visitFileClasses(String filePrefix, URL resource) throws IOException {
        visitFileClasses(filePrefix, new File(resource.getFile()));
    }

    private void visitFileClasses(String filePrefix, File directory) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                visitFileClasses(filePrefix + file.getName() + "/", file);
            }
            else if (file.isFile()) {
                visitClass(filePrefix, filePrefix + file.getName());
            }
        }
    }

    private void visitClass(String filePrefix, String name) {
        if (name.startsWith(filePrefix) && name.endsWith(".class")) {
            try {
                Class<?> clazz = Class.forName(getClassName(name));
                if (filter.test(clazz)) {
                    visitor.accept(clazz);
                }
            }
            catch (Throwable e) {
                logger.debug("class not found: " + name);
            }
        }
    }

    private String getClassName(String name) {
        return name.substring(0, name.length()-6).replace('/', '.');
    }

    private String getDirectoryName(String basePackage) {
        String filePrefix = basePackage.replace('.', '/');
        return filePrefix.endsWith("/") ? filePrefix : filePrefix + "/";
    }
}