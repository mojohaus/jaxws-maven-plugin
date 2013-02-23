/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle licenses this file to You under the Apache License, Version 2.0
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
 *
 * 
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2006 Guillaume Nodet
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
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
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
     * Specify that a WSDL file should be generated in <code>${resourceDestDir}</code>.
     */
    @Parameter(defaultValue = "false")
    protected boolean genWsdl;

    /**
     * Service endpoint implementation class name.
     */
    @Parameter
    private String sei;

    /**
     * Used in conjunction with <code>genWsdl<code> to specify the protocol to use in the
     * <code>wsdl:binding</code>. Valid values are "<code>soap1.1</code>" or "<code>Xsoap1.2</code>",
     * default is "<code>soap1.1</code>". "<code>Xsoap1.2</code>" is not standard
     * and can only be used in conjunction with the <code>extension</code> option.
     */
    @Parameter
    private String protocol;

    /**
     * List of plugin artifacts.
     */
    @Parameter(property = "plugin.artifacts", readonly = true)
    private List<Artifact> pluginArtifacts;

    /**
     * Specify the Service name to use in the generated WSDL.
     * Used in conjunction with the <code>genWsdl</code> option.
     */
    @Parameter
    private String servicename;

    /**
     * Specify the Port name to use in the generated WSDL.
     * Used in conjunction with the <code>genWsdl</code> option.
     */
    @Parameter
    private String portname;

    /**
     * Inline schemas in the generated WSDL.
     * Used in conjunction with the <code>genWsdl</code> option.
     */
    @Parameter(defaultValue = "false")
    private boolean inlineSchemas;

    /**
     * Turn off compilation after code generation and let generated sources be
     * compiled by maven during compilation phase; keep is turned on with this option.
     */
    @Parameter(defaultValue = "false")
    private boolean xnocompile;

    /**
     */
    @Parameter(defaultValue = "false")
    private boolean xdonotoverwrite;
    
    protected abstract File getResourceDestDir();

    protected abstract File getClassesDir();

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException {

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

            File resourceDir = getResourceDestDir();
            resourceDir.mkdirs();
            args.add("-r");
            args.add(resourceDir.getAbsolutePath());
            Resource r = new Resource();
            r.setDirectory(getRelativePath(project.getBasedir(), getResourceDestDir()));
            project.addResource(r);

        }

        if (xdonotoverwrite) {
            args.add("-Xdonotoverwrite");
        }

        args.add(aSei);

        getLog().debug("jaxws:wsgen args: " + args);

        return args;
    }

    private String getRelativePath(File root, File f) {
        return root.toURI().relativize(f.toURI()).getPath();
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
