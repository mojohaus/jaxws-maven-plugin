/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Copyright 2006 Codehaus
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * 
 * @author dantran <dantran@apache.org>
 * @version $Id: AbstractJaxwsMojo.java 3240 2007-02-04 07:13:21Z dantran $ *
 */
abstract class AbstractJaxwsMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * Output messages about what the tool is doing.
     * 
     * @parameter default-value="false"
     */
    protected boolean verbose;

    /**
     * Keep generated files.
     * 
     * @parameter default-value="true"
     */
    protected boolean keep;

    /**
     * Allow to use the JAXWS Vendor Extensions.
     * 
     * @parameter default-value="false"
     */
    protected boolean extension;

    /**
     * Specify character encoding used by source files.
     *
     * @parameter default-value="${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * Specify optional command-line options.
     * <p>
     * Multiple elements can be specified, and each token must be placed in its own list.
     * </p>
     * @parameter
     */
    private List<String> args;

    /**
     * Turn off compilation after code generation and let generated sources be
     * compiled by maven during compilation phase; keep is turned on with this option.
     *
     * @parameter default-value="true"
     */
    private boolean xnocompile;

    /**
     * Map of of plugin artifacts.
     *
     * @parameter expression="${plugin.artifactMap}"
     * @readonly
     */
    private Map pluginArtifactMap;

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    protected abstract File getDestDir();

    protected abstract File getSourceDestDir();

    protected abstract void addSourceRoot(String sourceDir);

    protected abstract File getDefaultSrcOut();

    /**
     * Need to build a URLClassloader since Maven removed it form the chain
     * @param parent
     * @return
     */
    protected String initClassLoader( ClassLoader parent )
        throws MojoExecutionException
    {

        try
        {
            List classpathFiles = project.getCompileClasspathElements();
            
            URL[] urls = new URL[classpathFiles.size() + 3];
            
            StringBuffer classPath = new StringBuffer();
            
            for ( int i = 0; i < classpathFiles.size(); ++i )
            {
                getLog().debug( (String) classpathFiles.get( i ) );
                urls[i] = new File( (String) classpathFiles.get( i ) ).toURI().toURL();
                classPath.append( (String) classpathFiles.get( i ) );
                classPath.append( File.pathSeparatorChar );
            }

            
            urls[classpathFiles.size()] = new File( project.getBuild().getOutputDirectory() ).toURI().toURL();

            Artifact jaxwsToolsArtifact = (Artifact) pluginArtifactMap.get( "com.sun.xml.ws:jaxws-tools" );
            urls[classpathFiles.size() + 1] = jaxwsToolsArtifact.getFile().toURI().toURL();
            
            File toolsJar = new File( System.getProperty( "java.home"), "../lib/tools.jar" );
            if ( ! toolsJar.exists() ) 
            {
            	//
            	toolsJar = new File( System.getProperty( "java.home"), "lib/tools.jar" );
            }
            urls[classpathFiles.size() + 2] = toolsJar.toURI().toURL();
            
            URLClassLoader cl = new URLClassLoader( urls, parent );

            // Set the new classloader
            Thread.currentThread().setContextClassLoader( cl );

            System.setProperty( "java.class.path", classPath.toString() );

            String sysCp = System.getProperty( "java.class.path" );

            return sysCp;            
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

    }

    protected List<String> getCommonArgs() throws MojoExecutionException {
        List<String> commonArgs = new ArrayList<String>();

        if (!isDefaultSrc(getSourceDestDir()) || keep) {
            commonArgs.add("-keep");
            commonArgs.add("-s");
            commonArgs.add(getSourceDestDir().getAbsolutePath());
            getSourceDestDir().mkdirs();
            addSourceRoot(getSourceDestDir().getAbsolutePath());
        }

        File destDir = getDestDir();
        if (xnocompile && isDefaultOut(getDestDir())) {
            destDir = null;//new File(project.getBuild().getDirectory(), "dummy-ws");
        }
        if (destDir != null) {
            destDir.mkdirs();
            commonArgs.add("-d");
            commonArgs.add(destDir.getAbsolutePath());
        }

        if (verbose) {
            commonArgs.add("-verbose");
        }

        if (isArgSupported("-encoding")) {
            if (encoding != null) {
                commonArgs.add("-encoding");
                commonArgs.add(encoding);
            } else {
                getLog().warn("Using platform encoding (" + System.getProperty("file.encoding") + "), build is platform dependent!");
            }
        }

        if (extension) {
            commonArgs.add("-extension");
        }

        if(xnocompile){
            commonArgs.add("-Xnocompile");
        }

        // add additional command line options
        if (args != null) {
            for (String arg : args) {
                commonArgs.add(arg);
            }
        }
        return commonArgs;
    }
    
    protected boolean isArgSupported(String arg) throws MojoExecutionException {
        boolean isSupported = true;
        Artifact a = (Artifact) pluginArtifactMap.get("com.sun.xml.ws:jaxws-tools");
        String v = null;
        try {
            ArtifactVersion av = a.getSelectedVersion();
            v = av.toString();
            //2.2.6+
            if ("-encoding".equals(arg)) {
                isSupported = av.getMajorVersion() >= 2 && av.getMinorVersion() >= 2 && av.getIncrementalVersion() >= 6;
            }
        } catch (OverConstrainedVersionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        if (!isSupported) {
            getLog().warn("'" + arg + "' is not supported by jaxws-tools:" + v);
        }
        return isSupported;
    }

    private boolean isDefaultOut(File wsout) {
        Build b = project.getBuild();
        String out = b.getOutputDirectory();
        String testOut = b.getTestOutputDirectory();
        return wsout.equals(new File(out)) || wsout.equals(new File(testOut));
    }

    private boolean isDefaultSrc(File srcout) {
        return srcout.equals(getDefaultSrcOut());
    }
}
