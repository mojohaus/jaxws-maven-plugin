/*
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
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
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
     * Output messages about what the tool is doing
     * 
     * @parameter default-value="false"
     */
    protected boolean verbose;

    /**
     * Keep generated files.
     * 
     * @parameter default-value="false"
     */
    protected boolean keep;

    /**
     * Allow to use the JAXWS Vendor Extensions.
     * 
     * @parameter default-value="false"
     */
    protected boolean extension;


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
                urls[i] = new File( (String) classpathFiles.get( i ) ).toURL();
                classPath.append( (String) classpathFiles.get( i ) );
                classPath.append( File.pathSeparatorChar );
            }

            
            urls[classpathFiles.size()] = new File( project.getBuild().getOutputDirectory() ).toURL();

            Artifact jaxwsToolsArtifact = (Artifact) pluginArtifactMap.get( "com.sun.xml.ws:jaxws-tools" );
            urls[classpathFiles.size() + 1] = jaxwsToolsArtifact.getFile().toURL();
            
            File toolsJar = new File( System.getProperty( "java.home"), "../lib/tools.jar" );
            if ( ! toolsJar.exists() ) 
            {
            	//
            	toolsJar = new File( System.getProperty( "java.home"), "lib/tools.jar" );
            }
            urls[classpathFiles.size() + 2] = toolsJar.toURL();
            
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
}
