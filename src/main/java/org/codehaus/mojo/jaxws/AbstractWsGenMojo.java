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

package org.codehaus.mojo.jaxws;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
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
 * @author gnodet (gnodet@apache.org)
 * @author dantran (dantran@apache.org)
 */
abstract class AbstractWsGenMojo
    extends AbstractJaxwsMojo
{

    public enum SeiLookup {
        classpath,
        javassist
    }
    /**
     * Specify that a WSDL file should be generated in <code>${resourceDestDir}</code>.
     */
    @Parameter( defaultValue = "false" )
    protected boolean genWsdl;

    /**
     * Service endpoint implementation class name.
     */
    @Parameter
    private String sei;

    /**
     * Service endpoint implementation lookup method. Valid values are: "<code>classpath</code>" which is default and
     * searches plugin classpath, "<code>javassist</code>" reads class bytecode and determines if class is service
     * endpoint implementation
     */
    @Parameter(defaultValue = "classpath")
    private SeiLookup seiLookup;

    /**
     * Used in conjunction with <code>genWsdl<code> to specify the protocol to use in the
     * <code>wsdl:binding</code>. Valid values are "<code>soap1.1</code>" or "<code>Xsoap1.2</code>",
     * default is "<code>soap1.1</code>". "<code>Xsoap1.2</code>" is not standard
     * and can only be used in conjunction with the <code>extension</code> option.
     */
    @Parameter
    private String protocol;

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
    @Parameter( defaultValue = "false" )
    private boolean inlineSchemas;

    /**
     * Turn off compilation after code generation and let generated sources be
     * compiled by maven during compilation phase; keep is turned on with this option.
     */
    @Parameter( defaultValue = "false" )
    private boolean xnocompile;

    /**
     */
    @Parameter( defaultValue = "false" )
    private boolean xdonotoverwrite;

    /**
     * Metadata file for wsgen. See
     * <a href="https://jax-ws.java.net/2.2.8/docs/ch03.html#users-guide-external-metadata">the JAX-WS Guide</a>
     * for the description of this feature.
     * Unmatched files will be ignored.
     *
     * @since 2.3
     * @see <a href="https://jax-ws.java.net/2.2.8/docs/ch03.html#users-guide-external-metadata">External Web Service Metadata</a>
     */
    @Parameter
    private File metadata;

    protected abstract File getResourceDestDir();

    protected abstract File getClassesDir();

    private FileStateRegistry fileStateRegistry;

    @Override
    public void executeJaxws()
        throws MojoExecutionException, MojoFailureException
    {
        fileStateRegistry = FileStateRegistry.load(new File(project.getBuild().getDirectory(), "maven-status/" + pluginDescriptor.getArtifactId() + "/file.status"));

        Set<Sei> seis = new HashSet<Sei>();
        if ( sei != null )
        {
            seis.add( new Sei(sei) );
        }
        else
        {
            // find all SEIs within current classes
            if(seiLookup == SeiLookup.classpath) {
                seis.addAll(getSEIs(getClassesDir()));
            } else if(seiLookup == SeiLookup.javassist) {
                seis.addAll(getSEIsJavassist(getClassesDir()) );
            } else {
                throw new MojoFailureException( "Not supported service endpoint lookup method " + seiLookup );
            }
        }
        if ( seis.isEmpty() )
        {
            throw new MojoFailureException( "No @javax.jws.WebService found." );
        }
        for ( Sei aSei : seis )
        {
            if(aSei.isChanged || wsdlRebuild(aSei) || jaxwsRebuild(aSei)) {
                processSei(aSei.name);
                if(aSei.classFile != null) {
                    fileStateRegistry.store(aSei.classFile);
                }
            } else {
                getLog().info( "Wsgen not needed because service endpoint interface " + aSei.name + " did not change from last compilation.");
            }
        }
    }

    /**
     * Checks if jaxws folder is missing or empty
     * @param sei
     * @return <code>true</code> if jaxws folder is missing or empty otherwise <code>false</code>
     */
    private boolean jaxwsRebuild(Sei sei){
        File jaxwsDir = sei.jaxwsDir(getDestDir());
        return !jaxwsDir.exists() || jaxwsDir.list().length == 0;
    }

    /**
     * Checks if wsdl folder is missing or empty in case of wsdl generation
     * @param sei
     * @return <code>true</code> if wsdl generation is turend on and wsdl folder is missing or empty otherwise
     * <code>false</code>
     */
    private boolean wsdlRebuild(Sei sei){
        return this.genWsdl && (!getResourceDestDir().exists() || getResourceDestDir().list().length == 0);
    }

    protected void processSei( String aSei )
        throws MojoExecutionException
    {
        getLog().info( "Processing: " + aSei );
        ArrayList<String> args = getWsGenArgs( aSei );
        getLog().info( "jaxws:wsgen args: " + args );
        exec( args );
        if ( metadata != null )
        {
            try
            {
                FileUtils.copyFileToDirectory( metadata, getClassesDir() );
            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException( ioe.getMessage(), ioe );
            }
        }
    }

    @Override
    protected String getMain()
    {
        return "com.sun.tools.ws.wscompile.WsgenTool";
    }

    @Override
    protected String getToolName()
    {
        return "wsgen";
    }

    @Override
    protected String getExtraClasspath()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( getClassesDir().getAbsolutePath() );
        for ( Artifact a : project.getArtifacts() )
        {
            buf.append( File.pathSeparatorChar );
            buf.append( a.getFile().getAbsolutePath() );
        }
        return buf.toString();
    }

    @Override
    protected boolean isXnocompile()
    {
        return xnocompile;
    }

    /**
     * Returns wsgen's command arguments as a list
     */
    private ArrayList<String> getWsGenArgs( String aSei )
        throws MojoExecutionException
    {
        ArrayList<String> args = new ArrayList<String>();
        args.addAll( getCommonArgs() );

        if ( this.genWsdl )
        {
            if ( this.protocol != null )
            {
                args.add( "-wsdl:" + this.protocol );
            }
            else
            {
                args.add( "-wsdl" );
            }

            if ( inlineSchemas )
            {
                maybeUnsupportedOption( "-inlineSchemas", null, args );
            }

            if ( servicename != null )
            {
                args.add( "-servicename" );
                args.add( servicename );
            }

            if ( portname != null )
            {
                args.add( "-portname" );
                args.add( portname );
            }

            File resourceDir = getResourceDestDir();
            if ( !resourceDir.mkdirs() && !resourceDir.exists() )
            {
                getLog().warn( "Cannot create directory: " + resourceDir.getAbsolutePath() );
            }
            args.add( "-r" );
            args.add( "'" + resourceDir.getAbsolutePath() + "'" );
            if ( !"war".equals( project.getPackaging() ) )
            {
                Resource r = new Resource();
                r.setDirectory( getRelativePath( project.getBasedir(), getResourceDestDir() ) );
                project.addResource( r );
            }
        }

        if ( xdonotoverwrite )
        {
            args.add( "-Xdonotoverwrite" );
        }

        if ( metadata != null && isArgSupported( "-x" ) )
        {
            maybeUnsupportedOption( "-x", "'" + metadata.getAbsolutePath() + "'", args );
        }

        args.add( aSei );

        return args;
    }

    private String getRelativePath( File root, File f )
    {
        return root.toURI().relativize( f.toURI() ).getPath();
    }

    private Set<Sei> getSEIsJavassist(File directory)
            throws MojoExecutionException, MojoFailureException {
        Set<Sei> seis = new HashSet<Sei>();
        if (!directory.exists() || directory.isFile()) {
            return seis;
        }

        try {
            for (String s : FileUtils.getFileAndDirectoryNames(directory, "**/*.class", null, true, true, true,
                    false)) {
                File classFile = new File(s);
                Sei.Type type = Sei.findWebserviceAnnotation(classFile);
                if (!type.classFile.isInterface() && type.webservice != null) {
                    // more sophisticated checks are done by wsgen itself
                    boolean isChanged = fileStateRegistry.isChanged(classFile);
                    seis.add(new Sei(isChanged, type.classFile.getName(), type.webservice, classFile));
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        return seis;
    }

    private Set<Sei> getSEIs( File directory )
            throws MojoExecutionException, MojoFailureException {
        Set<Sei> seis = new HashSet<Sei>();
        if ( !directory.exists() || directory.isFile() )
        {
            return seis;
        }
        ClassLoader cl = null;
        try
        {
            cl = new URLClassLoader( new URL[] { directory.toURI().toURL() } );
            for ( String s : FileUtils.getFileAndDirectoryNames( directory, "**/*.class", null, false, true, true,
                                                                 false ) )
            {
                try
                {
                    File classFile = new File(directory, s);
                    String clsName = s.replace( File.separator, "." );
                    Class<?> c = cl.loadClass( clsName.substring( 0, clsName.length() - 6 ) );
                    WebService ann = c.getAnnotation( WebService.class );
                    if ( !c.isInterface() && ann != null )
                    {
                        // more sophisticated checks are done by wsgen itself
                        boolean isChanged = fileStateRegistry.isChanged(classFile);
                        seis.add( new Sei(isChanged, c.getName(), ann, classFile));
                    }
                }
                catch ( ClassNotFoundException ex )
                {
                    throw new MojoExecutionException( ex.getMessage(), ex );
                }
            }
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( ex.getMessage(), ex );
        }
        finally
        {
            closeQuietly( cl );
        }
        return seis;
    }
}
