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
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    protected File getDestDir() {
        return destDir;
    }
}
