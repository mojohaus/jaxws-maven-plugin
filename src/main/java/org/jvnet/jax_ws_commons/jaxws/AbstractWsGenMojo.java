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

import com.sun.tools.ws.WsGen;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jws.WebService;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * 
 *
 * @author gnodet <gnodet@apache.org>
 * @author dantran <dantran@apache.org>
 * @version $Id: WsGenMojo.java 3169 2007-01-22 02:51:29Z dantran $
 */
abstract class AbstractWsGenMojo extends AbstractJaxwsMojo {

    /**
     * Specify that a WSDL file should be generated in ${resourceDestDir}
     * 
     * @parameter default-value="false"
     */
    private boolean genWsdl;

    /**
     * Directory containing the generated wsdl files.
     * 
     * @parameter default-value="${project.build.directory}/jaxws/wsgen/wsdl"
     */
    private File resourceDestDir;

    /**
     * service endpoint implementation class name.
     * 
     * @parameter 
     */
    private String sei;

    /**
     * Used in conjunction with genWsdl to specify the protocol to use in the 
     * wsdl:binding.  Value values are "soap1.1" or "Xsoap1.2", default is "soap1.1". 
     * "Xsoap1.2" is not standard and can only be used in conjunction with the 
     * -extensions option
     * 
     * @parameter 
     */
    private String protocol;

    /**
     * List of plugin artifacts.
     *
     * @parameter expression="${plugin.artifacts}"
     * @readonly
     */
    private List<Artifact> pluginArtifacts;

    /**
     * Specify where to place generated source files, keep is turned on with this option. 
     * 
     * @parameter 
     */
    private File sourceDestDir;
    //default-value="${project.build.directory}/jaxws/java"

    /**
     * Specify the Service name to use in the generated WSDL.
     * Used in conjunction with the -wsdl option.
     *
     * @parameter
     */
    private String servicename;

    /**
     * Specify the Port name to use in the generated WSDL.
     * Used in conjunction with the -wsdl option.
     *
     * @parameter
     */
    private String portname;

    /**
     * Inline schemas in the generated wsdl.
     * Used in conjunction with the -wsdl option.
     *
     * @parameter default-value="false"
     */
    private boolean inlineSchemas;

    /**
     *
     * @parameter default-value="false"
     */
    private boolean xdonotoverwrite;

    @Override
    protected File getSourceDestDir() {
        return sourceDestDir;
    }

    protected abstract File getClassesDir();
    
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException {
        init();

        // Need to build a URLClassloader since Maven removed it form the chain
        ClassLoader parent = this.getClass().getClassLoader();
        String orginalSystemClasspath = this.initClassLoader(parent);

        Set<String> seis = new HashSet<String>();
        if (sei != null) {
            seis.add(sei);
        } else {
            //find all SEIs within current classes
            seis.addAll(getSEIs(getClassesDir()));
        }

        if (seis.isEmpty()) {
            throw new MojoFailureException("No @javax.jws.WebService found.");
        }

        try {
            for (String aSei : seis) {
                ArrayList<String> args = getWsGenArgs(aSei);

                if (WsGen.doMain(args.toArray(new String[args.size()])) != 0)
                    throw new MojoExecutionException("Error executing: wsgen " + args);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Throwable e) {
            throw new MojoExecutionException("Failed to execute wsgen",e);
        } finally {
            // Set back the old classloader
            Thread.currentThread().setContextClassLoader(parent);
            System.setProperty("java.class.path", orginalSystemClasspath);
        }
    }

    private void init() throws MojoExecutionException, MojoFailureException {
        if (!getDestDir().exists())
            getDestDir().mkdirs();
    }

    /**
     * Construct wsgen arguments
     * @return a list of arguments
     * @throws MojoExecutionException
     */
    private ArrayList<String> getWsGenArgs(String aSei)
        throws MojoExecutionException {
        ArrayList<String> args = new ArrayList<String>();
        args.addAll(getCommonArgs());

        args.add("-cp");
        StringBuilder buf = new StringBuilder();
        buf.append(getDestDir().getAbsolutePath());
        for (Artifact a : (Set<Artifact>)project.getArtifacts()) {
            buf.append(File.pathSeparatorChar);
            buf.append(a.getFile().getAbsolutePath());
        }
        for (Artifact a : pluginArtifacts) {
            buf.append(File.pathSeparatorChar);
            buf.append(a.getFile().getAbsolutePath());
        }
        args.add(buf.toString());

        if (this.genWsdl) {
            if (this.protocol != null) {
                args.add("-wsdl:" + this.protocol);
            } else {
                args.add("-wsdl");
            }

            if (inlineSchemas) {
                args.add("-inlineSchemas");
            }

            if (servicename != null) {
                args.add("-servicename");
                args.add(servicename);
            }

            if (portname != null) {
                args.add("-portname");
                args.add(portname);
            }
            
            args.add("-r");
            args.add(this.resourceDestDir.getAbsolutePath());
            this.resourceDestDir.mkdirs();

        }

        if (xdonotoverwrite) {
            args.add("-Xdonotoverwrite");
        }

        args.add(aSei);

        getLog().debug("jaxws:wsgen args: " + args);

        return args;
    }

    private Set<String> getSEIs(File directory) throws MojoExecutionException {
        Set<String> seis = new HashSet<String>();
        if (!directory.exists() || directory.isFile()) {
            return seis;
        }
        ClassLoader cl = null;
        try {
            cl = new URLClassLoader(new URL[]{directory.toURI().toURL()});
            for (String s : (List<String>) FileUtils.getFileAndDirectoryNames(directory, "**/*.class", null, false, true, true, false)) {
                try {
                    String clsName = s.replace(File.separator, ".");
                    Class<?> c = cl.loadClass(clsName.substring(0, clsName.length() - 6));
                    WebService ann = c.getAnnotation(WebService.class);
                    if (!c.isInterface() && ann != null) {
                        //more sophisticated checks are done by wsgen itself
                        seis.add(c.getName());
                    }
                } catch (ClassNotFoundException ex) {
                    throw new MojoExecutionException(ex.getMessage(), ex);
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } finally {
            if (cl != null && cl instanceof Closeable) {
                try {
                    ((Closeable) cl).close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
        return seis;
    }
}
