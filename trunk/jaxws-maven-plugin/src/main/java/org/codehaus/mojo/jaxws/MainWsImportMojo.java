/*
 * Copyright 2007-2011 JAX-WS RI team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.mojo.jaxws;

import java.io.File;

/**
 * Parses wsdl and binding files and generates Java code needed to access it.
 *
 * @goal wsimport
 * @phase generate-sources
 * @requiresDependencyResolution
 * @description JAXWS 2.x Plugin.
 *
 * @author Kohsuke Kawaguchi
 */
public class MainWsImportMojo extends WsImportMojo {

    /**
     * Specify where to place output generated classes
     * Set to "" to turn it off
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected File destDir;

    /**
     * Specify where to place generated source files, keep is turned on with this option.
     *
     * @parameter default-value="${project.build.directory}/jaxws/wsimport/java"
     */
    private File sourceDestDir;

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    protected File getDestDir() {
        return destDir;
    }

    protected File getSourceDestDir() {
        return sourceDestDir;
    }

    @Override
    protected void addSourceRoot(String sourceDir) {
        project.addCompileSourceRoot(sourceDir);
    }
}
