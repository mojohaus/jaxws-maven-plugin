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
import java.io.IOException;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
     * Specify where to place output generated classes. Use <code>xnocompile</code>
     * to turn this off.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File destDir;

    /**
     * Specify where to place generated source files, keep is turned on with this option.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/wsgen
     */
    private File sourceDestDir;

    /**
     * Directory containing the generated wsdl files.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/wsdl
     */
    private File resourceDestDir;

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    @Override
    protected File getDestDir() {
        return destDir;
    }

    @Override
    protected File getSourceDestDir() {
        return sourceDestDir;
    }

    @Override
    protected void addSourceRoot(String sourceDir) {
        project.addCompileSourceRoot(sourceDir);
    }

    @Override
    protected File getResourceDestDir() {
        return resourceDestDir;
    }

    @Override
    protected File getDefaultSrcOut() {
        return new File(project.getBuild().getDirectory(), "generated-sources/wsgen");
    }

    @Override
    protected File getClassesDir() {
        return new File(project.getBuild().getOutputDirectory());
    }
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        if (genWsdl) {
            try {
                attachWsdl();
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to execute wsgen", ex);
            }
        }

    }

    private void attachWsdl() throws IOException {
        File target = new File(project.getBuild().getDirectory());
        if (!"war".equalsIgnoreCase(project.getPackaging())) {
            // META-INF/wsdl for jar etc packagings
            target = new File(project.getBuild().getOutputDirectory(), "META-INF/wsdl");
        } else {
            // WEB-INF/wsdl for war
            String targetPath = null;
            Plugin war = (Plugin) project.getBuild().getPluginsAsMap().get("org.apache.maven.plugins:maven-war-plugin");
            for (PluginExecution exec : war.getExecutions()) {
                //check execution/configuration
                String s = getWebappDirectory(exec.getConfiguration());
                if (s != null) {
                    targetPath = s;
                    break;
                }
            }
            if (targetPath == null) {
                //check global plugin configuration
                targetPath = getWebappDirectory(war.getConfiguration());
            }
            target = targetPath != null ? new File(targetPath) : new File(target, project.getBuild().getFinalName());
            target = new File(target, "WEB-INF/wsdl");
        }
        target.mkdirs();
        getLog().debug("Packaging WSDL(s) to: " + target);
        FileUtils.copyDirectory(getResourceDestDir(), target);
    }

    private String getWebappDirectory(Object conf) {
        if (conf == null) {
            return null;
        }
        Xpp3Dom el = ((Xpp3Dom) conf).getChild("webappDirectory");
        return el != null ? el.getValue() : null;
    }

}