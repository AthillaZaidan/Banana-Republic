package com.bananarepublic.plugin;

import com.bananarepublic.model.card.DevelopmentCard;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {

    public List<DevelopmentCard> loadFromJar(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new PluginLoadException("JAR file does not exist or is not a file: " + jarFile);
        }

        List<DevelopmentCard> cards = new ArrayList<>();
        URLClassLoader classLoader = null;

        try {
            URL jarUrl = jarFile.toURI().toURL();
            classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

            List<String> classNames = findExperimentCardClasses(jarFile, classLoader);

            if (classNames.isEmpty()) {
                throw new PluginLoadException("No ExperimentCard implementation found in JAR: " + jarFile);
            }

            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className, true, classLoader);

                    if (!ExperimentCard.class.isAssignableFrom(clazz)) {
                        throw new PluginLoadException("Class " + className + " does not implement ExperimentCard");
                    }

                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    ExperimentCard experimentCard = (ExperimentCard) instance;
                    cards.add(new PluginExperimentCardAdapter(experimentCard));
                } catch (PluginLoadException e) {
                    throw e;
                } catch (Exception e) {
                    throw new PluginLoadException("Failed to instantiate class " + className, e);
                }
            }

        } catch (PluginLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginLoadException("Failed to load plugin JAR: " + jarFile, e);
        } finally {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (IOException ignored) {
                }
            }
        }

        return cards;
    }

    private List<String> findExperimentCardClasses(File jarFile, ClassLoader classLoader) {
        List<String> classNames = new ArrayList<>();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className, true, classLoader);
                        if (ExperimentCard.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            classNames.add(className);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    }
                }
            }
        } catch (IOException e) {
            throw new PluginLoadException("Failed to read JAR entries", e);
        }

        return classNames;
    }
}
