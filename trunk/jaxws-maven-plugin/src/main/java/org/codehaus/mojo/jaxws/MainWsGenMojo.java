package org.codehaus.mojo.jaxws;

import java.io.File;

/**
 * Reads a JAX-WS service endpoint implementation class
 * and generates all of the portable artifacts for a JAX-WS web service.
 *
 * @goal wsgen
 * @phase process-classes
 * @requiresDependencyResolution
 * @description generate JAX-WS wrapper beans. 
 */
public class MainWsGenMojo extends AbstractWsGenMojo {
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
