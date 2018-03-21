/**
 * MIT License
 *
 * Copyright (c) 2018 John Poth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
public class MyMojo extends AbstractMojo
{

    @Parameter(defaultValue = "${project.runtimeClasspathElements}", property = "rcp", required = true)
    private List<String> runtimeClasspathElements;

    public void execute() throws MojoExecutionException {
        String cp = runtimeClasspathElements.stream().reduce(runtimeClasspathElements.get(0), (a, b) -> a + ":" + b);
        System.out.println("Using rcp:" + cp);
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
