package org.dromara.autotable.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.Ignore;
import org.dromara.autotable.core.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于注解扫描java类
 *
 * @author don
 */
@Slf4j
public abstract class AutoTableClassScanner {

    public Set<Class<?>> scan(String[] basePackages) {

        if (basePackages == null) {
            return Collections.emptySet();
        }

        Set<Class<? extends Annotation>> includeAnnotations = getIncludeAnnotations();
        Set<Class<? extends Annotation>> excludeAnnotations = getExcludeAnnotations();

        // 经过自定义的拦截器，修改最终影响自动建表的注解
        AutoTableGlobalConfig.instance().getAutoTableAnnotationInterceptors().forEach(fn -> fn.intercept(includeAnnotations, excludeAnnotations));

        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder();

        return Arrays.stream(basePackages)
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .map(basePackage -> {
                    try {
                        return getClasses(basePackage,
                                clazz -> includeAnnotations.stream().anyMatch(anno -> autoTableAnnotationFinder.exist(clazz, anno)) &&
                                        excludeAnnotations.stream().noneMatch(anno -> autoTableAnnotationFinder.exist(clazz, anno))
                        );
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(String.format("扫描包%s下实体出错", basePackage), e);
                    }
                }).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    protected Set<Class<? extends Annotation>> getExcludeAnnotations() {
        return new HashSet<>(Collections.singleton(Ignore.class));
    }

    protected Set<Class<? extends Annotation>> getIncludeAnnotations() {
        return new HashSet<>(Collections.singletonList(AutoTable.class));
    }

    protected Set<Class<?>> getClasses(String packageName, Function<Class<?>, Boolean> checker) throws IOException, ClassNotFoundException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        String basePackage = path.split("/\\*")[0];
        String patternPackage = path
                // 多级路径
                .replace("**", "[A-Za-z0-9$_/]+")
                // 单级路径
                .replace("*", "[A-Za-z0-9$_]+");
        if (!patternPackage.endsWith("/")) {
            patternPackage += "/";
        }
        Pattern checkPattern = Pattern.compile("(" + patternPackage + "[A-Za-z0-9$_/]+)\\.class$");

        Enumeration<URL> resources = classLoader.getResources(basePackage);
        Set<Class<?>> classes = new HashSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                String decodedPath = URLDecoder.decode(resource.getFile(), "UTF-8");
                classes.addAll(findLocalClasses(checkPattern, new File(decodedPath), checker));
            } else if ("jar".equals(resource.getProtocol())) {
                JarURLConnection jarURLConnection = (JarURLConnection) resource.openConnection();
                classes.addAll(findJarClasses(checkPattern, jarURLConnection.getJarFile(), checker));
            }
        }
        return classes;
    }

    protected Set<Class<?>> findLocalClasses(Pattern checkPattern, File directory, Function<Class<?>, Boolean> checker) throws IOException {
        Set<Class<?>> classes = new HashSet<>();
        if (!directory.exists()) {
            return classes;
        }

        try (Stream<Path> pathStream = Files.walk(directory.toPath())) {
            pathStream
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                    .forEach(path -> {
                        try {
                            String pathUrl = path.toUri().toURL().toString();
                            Matcher matcher = checkPattern.matcher(pathUrl);
                            if (matcher.find()) {
                                String className = matcher.group(1).replace("/", ".");
                                Class<?> clazz = Class.forName(className);
                                if (checker.apply(clazz)) { // check annotation
                                    classes.add(clazz);
                                }
                            }
                        } catch (ClassNotFoundException | MalformedURLException ignore) {
                        }
                    });
        } catch (Exception e) {
            log.error("查找本地类错误", e);
        }
        return classes;
    }

    protected Set<Class<?>> findJarClasses(Pattern checkPattern, JarFile jarFile, Function<Class<?>, Boolean> checker) throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                Matcher matcher = checkPattern.matcher(entry.getName());
                if (matcher.find()) {
                    String className = matcher.group(1).replace("/", ".");
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                    }
                    if (clazz != null && checker.apply(clazz)) {
                        classes.add(clazz);
                    }
                }
            }
        }
        return classes;
    }
}
