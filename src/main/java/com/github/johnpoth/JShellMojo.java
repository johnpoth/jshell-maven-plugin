/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.johnpoth;

import javax.tools.Tool;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;


@Mojo( name = "run", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST )
public class JShellMojo extends AbstractMojo
{

    private static final pathSeparator;
    
    static 
    {
        //Guard against possible JVM that doesn't supply this property or user deletion of it for some reason
        //In this case then assume UNIX-style.
        String sep = System.getProperties().getProperty("path.separator");
        if(sep == null || sep.trim().length() == 0)
        {
            pathSeparator = ":";
        }
        else
        {
            pathSeparator = sep;
        }
    }

    @Parameter(defaultValue = "${project.runtimeClasspathElements}", property = "rcp", required = true)
    private List<String> runtimeClasspathElements;

    @Parameter(defaultValue = "${project.testClasspathElements}", property = "trcp", required = true)
    private List<String> testClasspathElements;

    @Parameter(defaultValue = "false", property = "testClasspath")
    private boolean testClasspath;

    public void execute() throws MojoExecutionException {
        String cp;
        if (testClasspath) {
            cp = testClasspathElements.stream().reduce(runtimeClasspathElements.get(0), (a, b) -> a + pathSeparator + b);
        } else {
            cp = runtimeClasspathElements.stream().reduce(runtimeClasspathElements.get(0), (a, b) -> a + pathSeparator + b);
        }
        getLog().debug("Using classpath:" + cp);
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
            String[] args = new String[]{"--class-path", cp};
            jshell.run(System.in, System.out, System.err, args);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
