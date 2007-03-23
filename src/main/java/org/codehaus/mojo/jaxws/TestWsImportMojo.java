package org.codehaus.mojo.jaxws;

import java.io.File;

/**
 * Parses wsdl and binding files and generates Java code needed to access it
 * (for tests.)
 *
 * @author Kohsuke Kawaguchi
 */
public class TestWsImportMojo extends WsImportMojo {
    /**
     * Specify where to place output generated classes
     * Set to "" to turn it off
     * @parameter default-value="${project.build.testOutputDirectory}"
     */
    protected File destDir;

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    protected File getDestDir() {
        return destDir;
    }
}
