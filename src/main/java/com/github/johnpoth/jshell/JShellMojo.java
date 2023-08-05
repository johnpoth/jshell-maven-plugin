/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.johnpoth.jshell;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.tools.Tool;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;


@Mojo(name = "run", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class JShellMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.runtimeClasspathElements}", property = "rcp", required = true)
    private List<String> runtimeClasspathElements;

    @Parameter(defaultValue = "${project.testClasspathElements}", property = "trcp", required = true)
    private List<String> testClasspathElements;

    @Parameter(property = "plugin.artifacts", required = true, readonly = true)
    private List<Artifact> pluginArtifacts;

    @Parameter(defaultValue = "false", property = "testClasspath")
    private boolean testClasspath;

    @Parameter(defaultValue = "true", property = "jshell.useProjectClasspath")
    private boolean useProjectClasspath;

    @Parameter(property = "jshell.class-path")
    private String classpath;

    @Parameter(property = "jshell.module-path")
    private String modulepath;

    @Parameter(property = "jshell.add-modules")
    private String addModules;

    @Parameter(property = "jshell.add-exports")
    private String addExports;

    @Parameter(property = "jshell.scripts")
    private List<String> scripts = new ArrayList<>();

    // additional options that may be added in future Java releases.
    @Parameter(property = "jshell.options")
    private List<String> options = new ArrayList<>();

    public void execute() throws MojoExecutionException {
        String cp = buildClasspath();
        getLog().debug("Using classpath: " + cp);
        Optional<Module> module = ModuleLayer.boot().findModule("jdk.jshell");
        ClassLoader classLoader = module.get().getClassLoader();
        // Until https://issues.apache.org/jira/browse/MNG-6371 is resolved
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ServiceLoader<Tool> sl = ServiceLoader.load(javax.tools.Tool.class);
            Tool jshell = sl.stream()
                    .filter(a -> a.get().name().equals("jshell"))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("No JShell service providers found!"))
                    .get();
            String[] args = addArguments(cp);
            int exitCode = jshell.run(System.in, System.out, System.err, args);
            if (exitCode != 0) {
                throw new MojoExecutionException("An error was encountered while executing. Exit code:" + exitCode);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private String buildClasspath() {
        final List<String> classpathElements = new ArrayList<>();
        if (testClasspath) {
            classpathElements.addAll(filterClasspath(testClasspathElements));
        } else {
            classpathElements.addAll(filterClasspath(runtimeClasspathElements));
        }

        if (!pluginArtifacts.isEmpty()) {
            classpathElements.addAll(
                    pluginArtifacts.stream()
                            .map(artifact -> artifact.getFile().getAbsolutePath())
                            .collect(Collectors.toList())
            );
        }
        return filterClasspath(classpathElements).stream()
                .reduce("", (a, b) -> a + File.pathSeparator + b);
    }

    private List<String> filterClasspath(List<String> cp) {
        return cp.stream()
                .filter(s -> {
                    Path path = Paths.get(s);
                    if (Files.notExists(path)) {
                        getLog().warn(String.format(
                                "Removing: %s from the classpath.%n" +
                                        "If this is unexpected, please make sure you correctly build the project beforehand by invoking the correct Maven build phase (usually `install`, `test-compile` or `compile`). For example:%n" +
                                        "mvn test-compile com.github.johnpoth:jshell-maven-plugin:%s:run%n" +
                                        "For more information visit https://github.com/johnpoth/jshell-maven-plugin",
                                s,
                                "1.3"
                        ));
                        return false;
                    }
                    if (Files.isDirectory(path)) {
                        return true;
                    }
                    if (s.endsWith(".jar")) {
                        return true;
                    }
                    getLog().debug("Removing: " + s + " from the classpath because it is unsupported in JShell.");
                    return false;
                }).collect(Collectors.toList());
    }

    private String[] addArguments(String cp) {
        List<String> args = new ArrayList<>();
        if (useProjectClasspath) {
            args.add("--class-path");
            args.add(cp);
        } else if (classpath != null) {
            args.add("--class-path");
            args.add(classpath);
        }
        if (modulepath != null) {
            args.add("--module-path");
            args.add(modulepath);
        }
        if (addModules != null) {
            args.add("--add-modules");
            args.add(addModules);
        }
        if (addExports != null) {
            args.add("--add-exports");
            args.add(addExports);
        }
        for (String option : this.options) {
            args.add(option);
        }
        for (String script : scripts) {
            args.add(script);
        }
        return args.toArray(new String[0]);
    }
}
