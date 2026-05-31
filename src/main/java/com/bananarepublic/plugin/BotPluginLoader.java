package com.bananarepublic.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BotPluginLoader {

    public PlayerStrategy loadFromJar(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new PluginLoadException("JAR file does not exist or is not a file: " + jarFile);
        }

        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                getClass().getClassLoader()
        )) {
            String className = findPluginClassName(jarFile, classLoader);
            Class<?> clazz = Class.forName(className, true, classLoader);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            return (PlayerStrategy) instance;
        } catch (PluginLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginLoadException("Failed to load bot plugin JAR: " + jarFile, e);
        }
    }

    private String findPluginClassName(File jarFile, ClassLoader classLoader) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }

                String className = name.replace('/', '.').substring(0, name.length() - 6);
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    if (PlayerStrategy.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        return className;
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    // Skip unrelated classes.
                }
            }
        } catch (IOException e) {
            throw new PluginLoadException("Failed to read bot plugin JAR entries", e);
        }

        throw new PluginLoadException("No PlayerStrategy implementation found in JAR: " + jarFile);
    }
}
