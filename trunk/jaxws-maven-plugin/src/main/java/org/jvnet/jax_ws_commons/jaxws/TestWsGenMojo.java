/*
 * Copyright 2006 Guillaume Nodet.
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
package org.jvnet.jax_ws_commons.jaxws;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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

    @Override
    protected File getClassesDir() {
        return new File(project.getBuild().getTestOutputDirectory());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //no need to invoke wsgen-test if tests themselves are skipped/ignorred
        boolean skipTests = Boolean.valueOf(System.getProperty("skipTests", "false"));
        boolean mavenTestSkip = Boolean.valueOf(System.getProperty("maven.test.skip", "false"));
        if (skipTests || mavenTestSkip) {
            getLog().info("Skipping tests, nothing to do.");
        } else {
            super.execute();
        }
    }
}
