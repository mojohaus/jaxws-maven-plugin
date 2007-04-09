package org.codehaus.mojo.jaxws;

import java.io.File;

/**
 * Reads a JAX-WS service endpoint implementation class
 * and generates all of the portable artifacts for a JAX-WS web service
 * (into the generate test source directroy.)
 *
 * <p>
 * Due to <a href="http://jira.codehaus.org/browse/MNG-1508">MNG-1508</a>, this requires 2.0.5 or higher.
 * </p>
 *
 * @goal wsgen-test
 * @phase process-test-classes
 * @requiresDependencyResolution
 * @description generate JAX-WS wrapper beans.
 */
public class TestWsGenMojo extends AbstractWsGenMojo {
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
