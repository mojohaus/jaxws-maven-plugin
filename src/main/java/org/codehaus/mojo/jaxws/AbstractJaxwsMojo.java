/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.codehaus.mojo.jaxws;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 *
 * @author dantran (dantran@apache.org)
 * @version $Id: AbstractJaxwsMojo.java 3240 2007-02-04 07:13:21Z dantran $ *
 */
abstract class AbstractJaxwsMojo
    extends AbstractMojo
{

    /**
     * The Maven Project Object.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * Output messages about what the tool is doing.
     */
    @Parameter( defaultValue = "false" )
    protected boolean verbose;

    /**
     * Keep generated files.
     */
    @Parameter( defaultValue = "true" )
    protected boolean keep;

    /**
     * Allow to use the JAXWS Vendor Extensions.
     */
    @Parameter( defaultValue = "false" )
    private boolean extension;

    /**
     * Specify character encoding used by source files.
     */
    @Parameter( defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;

    /**
     * Specify optional command-line options.
     * <p>
     * Multiple elements can be specified, and each token must be placed in its own list.
     * </p>
     */
    @Parameter
    private List<String> args;

    /**
     * Specify optional JVM options.
     * <p>
     * Multiple elements can be specified, and each token must be placed in its own list.
     * </p>
     */
    @Parameter
    private List<String> vmArgs;

    /**
     * Path to the executable. Should be either <code>wsgen</code> or <code>wsimport</code>
     * but basically any script which will understand passed in arguments
     * will work.
     *
     * @since 2.2.1
     */
    @Parameter
    private File executable;

    /**
     * Information about this plugin, used to lookup this plugin's dependencies
     * from the currently executing project.
     *
     * @since 2.3.1
     */
    @Parameter( defaultValue = "${plugin}", readonly = true )
    protected PluginDescriptor pluginDescriptor;

    /**
     * Entry point for toolchains, to get JDK toolchain
     */
    @Component
    private ToolchainManager toolchainManager;

    /**
     * If a JDK toolchain is found, by default, it is used to get <code>java</code> executable with its
     * <code>tools.jar</code>. But if set to <code>true</code>, it is used it to find <code>wsgen</code>
     * and <code>wsimport</code> executables.
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "false" )
    private boolean useJdkToolchainExecutable;

    /**
     * The current build session instance. This is used for toolchain manager API calls.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    private static final Logger logger = Logger.getLogger( AbstractJaxwsMojo.class.getName() );

    // arguments supported by Metro 2.2/JAXWS RI 2.2.6
    private static final List<String> METRO_22 = new ArrayList<String>();

    // arguments supported by Metro 2.2.1/JAXWS RI 2.2.7
    private static final List<String> METRO_221 = new ArrayList<String>();

    // arguments supported by Metro 2.3/JAXWS RI 2.2.8
    private static final List<String> METRO_23 = new ArrayList<String>();

    static
    {
        METRO_22.add( "-encoding" );
        METRO_22.add( "-clientjar" );
        METRO_22.add( "-generateJWS" );
        METRO_22.add( "-implDestDir" );
        METRO_22.add( "-implServiceName" );
        METRO_22.add( "-implPortName" );

        METRO_221.addAll( METRO_22 );
        METRO_221.add( "-XdisableAuthenticator" );

        METRO_23.addAll( METRO_221 );
        METRO_23.add( "-x" );
    }

    /**
     * Main class of the tool to launch when launched as java command.
     * @return the class name
     */
    protected abstract String getMain();

    /**
     * Name of the tool to run when launched as JDK executable from JDK Toolchain.
     * @return the tool name
     */
    protected abstract String getToolName();

    /**
     * Either <code>${build.outputDirectory}</code> or <code>${build.testOutputDirectory}</code>.
     * @return the destination directory
     */
    protected abstract File getDestDir();

    protected abstract File getSourceDestDir();

    protected void addSourceRoot( String sourceDir )
    {
        if ( !project.getCompileSourceRoots().contains( sourceDir ) )
        {
            getLog().debug( "adding src root: " + sourceDir );
            project.addCompileSourceRoot( sourceDir );
        }
        else
        {
            getLog().debug( "existing src root: " + sourceDir );
        }
    }

    protected abstract File getDefaultSrcOut();

    /**
     * Checks if compilation after code generation and let generated sources be
     * compiled by Maven during compilation phase.
     * @return true if compilation should not be done by the JAX-WS tool
     */
    protected abstract boolean isXnocompile();

    protected String getExtraClasspath()
    {
        return null;
    }

    protected boolean isExtensionOn()
    {
        return extension;
    }

    protected List<String> getCommonArgs()
        throws MojoExecutionException
    {
        List<String> commonArgs = new ArrayList<String>();

        if ( !isDefaultSrc( getSourceDestDir() ) || keep )
        {
            commonArgs.add( "-keep" );
            commonArgs.add( "-s" );
            commonArgs.add( "'" + getSourceDestDir().getAbsolutePath() + "'" );
            if ( !getSourceDestDir().mkdirs() && !getSourceDestDir().exists() )
            {
                getLog().warn( "Cannot create directory: " + getSourceDestDir().getAbsolutePath() );
            }
            addSourceRoot( getSourceDestDir().getAbsolutePath() );
        }

        File destDir = getDestDir();
        if ( !destDir.mkdirs() && !destDir.exists() )
        {
            getLog().warn( "Cannot create directory: " + destDir.getAbsolutePath() );
        }
        commonArgs.add( "-d" );
        commonArgs.add( "'" + destDir.getAbsolutePath() + "'" );

        if ( verbose )
        {
            commonArgs.add( "-verbose" );
        }

        if ( isArgSupported( "-encoding" ) )
        {
            if ( encoding != null )
            {
                maybeUnsupportedOption( "-encoding", encoding, commonArgs );
            }
            else
            {
                getLog().warn( "Using platform encoding (" + System.getProperty( "file.encoding" )
                    + "), build is platform dependent!" );
            }
        }

        if ( isExtensionOn() )
        {
            commonArgs.add( "-extension" );
        }

        if ( isXnocompile() )
        {
            commonArgs.add( "-Xnocompile" );
        }

        // add additional command line options
        if ( args != null )
        {
            for ( String arg : args )
            {
                commonArgs.add( arg );
            }
        }

        return commonArgs;
    }

    protected boolean isArgSupported( String arg )
        throws MojoExecutionException
    {
        // by default, use latest version supported args
        List<String> supportedArgs = METRO_23;

        // then try to find old known versions
        // try Metro first
        Artifact a = pluginDescriptor.getArtifactMap().get( "org.glassfish.metro:webservices-tools" );
        String v = null;
        if ( a != null )
        {
            ArtifactVersion av = getSelectedVersion( a );
            v = av.toString();
            if ( av.getMajorVersion() == 2 && av.getMinorVersion() == 2 )
            {
                supportedArgs = av.getIncrementalVersion() == 0 ? METRO_22 : METRO_221;
            }
        }
        else
        {
            // fallback to RI
            a = pluginDescriptor.getArtifactMap().get( "com.sun.xml.ws:jaxws-tools" );
            ArtifactVersion av = getSelectedVersion( a );
            v = av.toString();
            if ( av.getMajorVersion() == 2 && av.getMinorVersion() == 2 )
            {
                if ( av.getIncrementalVersion() == 6 )
                {
                    supportedArgs = METRO_22;
                }
                else if ( av.getIncrementalVersion() == 7 )
                {
                    supportedArgs = METRO_221;
                }
            }
        }

        boolean isSupported = supportedArgs.contains( arg );
        if ( !isSupported )
        {
            getLog().warn( "'" + arg + "' is not supported by " + a.getArtifactId() + ":" + v );
        }
        return isSupported;
    }

    private static ArtifactVersion getSelectedVersion( Artifact artifact )
        throws MojoExecutionException
    {
        try
        {
            return artifact.getSelectedVersion();
        }
        catch ( OverConstrainedVersionException ex )
        {
            throw new MojoExecutionException( ex.getMessage(), ex );
        }
    }

    private boolean isDefaultSrc( File srcout )
    {
        return srcout.equals( getDefaultSrcOut() );
    }

    @Override
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( ( executable == null ) && ( getJdkToolchain() != null ) && useJdkToolchainExecutable )
        {
            // get executable from JDK toolchain
            executable = new File( getJdkToolchain().findTool( getToolName() ) );
        }

        executeJaxws();
    }

    public abstract void executeJaxws()
        throws MojoExecutionException, MojoFailureException;

    protected void exec( List<String> arguments )
        throws MojoExecutionException
    {
        String launched = "";
        Commandline cmd = new Commandline();

        if ( executable != null )
        {
            // use JDK wsgen/wsimport or equivalent executable
            launched = executable.getName();
            if ( executable.isFile() && executable.canExecute() )
            {
                cmd.setExecutable( executable.getAbsolutePath() );
                if ( getExtraClasspath() != null )
                {
                    cmd.createArg().setLine( "-cp" );
                    cmd.createArg().setValue( getExtraClasspath() );
                }
            }
            else
            {
                throw new MojoExecutionException( "Cannot execute: " + executable.getAbsolutePath() );
            }
        }
        else
        {
            // use tool's class through Invoker as java execution
            launched = getMain();

            if ( ( getJdkToolchain() == null ) )
            {
                // use java executable from running Maven
                cmd.setExecutable( new File( new File( System.getProperty( "java.home" ), "bin" ),
                                             getJavaExec() ).getAbsolutePath() );
            }
            else
            {
                // use java executable from current JDK toolchain
                cmd.setExecutable( getJdkToolchain().findTool( "java" ) );
            }

            // add additional JVM options
            if ( vmArgs != null )
            {
                for ( String arg : vmArgs )
                {
                    cmd.createArg().setLine( arg );
                }
            }
            InvokerCP classpath = getInvokerCP();
            cmd.createArg().setValue( "-Xbootclasspath/p:" + classpath.ecp );
            cmd.createArg().setValue( "-cp" );
            cmd.createArg().setValue( classpath.invokerPath );
            cmd.createArg().setLine( Invoker.class.getCanonicalName() );
            cmd.createArg().setLine( getMain() );
            String extraCp = getExtraClasspath();
            String cp = ( ( extraCp != null ) ? ( extraCp + File.pathSeparator ) : "" ) + classpath.cp;
            try
            {
                File pathFile = createPathFile( cp );
                cmd.createArg().setLine( "-pathfile " + pathFile.getAbsolutePath() );
            }
            catch ( IOException ioe )
            {
                // creation of temporary file can fail, in such case just put everything on cp
                cmd.createArg().setValue( "-cp" );
                cmd.createArg().setValue( cp );
            }
        }

        cmd.setWorkingDirectory( project.getBasedir() );
        for ( String arg : arguments )
        {
            cmd.createArg().setLine( arg );
        }

        try
        {
            String fullCommand = cmd.toString();
            if ( isWindows() && 8191 <= fullCommand.length() )
            {
                getLog().warn( "Length of Windows command line is limited to 8191 characters, but current command has "
                    + fullCommand.length() + " characters:" );
                getLog().warn( fullCommand );
            }
            else
            {
                getLog().debug( fullCommand );
            }

            StreamConsumer sc = new DefaultConsumer();
            if ( CommandLineUtils.executeCommandLine( cmd, sc, sc ) != 0 )
            {
                throw new MojoExecutionException( "Invocation of " + launched + " failed - check output" );
            }
        }
        catch ( CommandLineException t )
        {
            throw new MojoExecutionException( t.getMessage(), t );
        }
    }

    protected void maybeUnsupportedOption( String option, String value, List<String> arguments )
    {
        if ( executable == null )
        {
            arguments.add( option );
            if ( value != null )
            {
                arguments.add( value );
            }
        }
        else
        {
            getLog().warn( option + " may not supported on older JDKs.\n"
                + "Use <args> to bypass this warning if you really want to use it." );
        }
    }

    /**
     * Calculates 3 classpaths used to launch tools as class through Invoker.
     * @return Invoker's classpath
     * @see Invoker
     */
    private InvokerCP getInvokerCP()
    {
        Set<Artifact> endorsedArtifacts = new HashSet<Artifact>();
        Map<String, Artifact> artifactsMap = new HashMap<String, Artifact>();
        for ( Artifact a : pluginDescriptor.getArtifacts() )
        {
            addArtifactToCp( a, artifactsMap, endorsedArtifacts );
        }

        StringBuilder cp = getCPasString( artifactsMap.values() );
        StringBuilder ecp = getCPasString( endorsedArtifacts );

        String invokerPath = null;
        try
        {
            invokerPath = Invoker.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            invokerPath = new URI( invokerPath.substring( 5 ) ).getPath();
        }
        catch ( URISyntaxException ex )
        {
            throw new RuntimeException( ex );
        }

        // add custom invoker path to normal classpath
        cp.append( File.pathSeparator );
        cp.append( invokerPath );

        // don't forget tools.jar
        String javaHome = getJavaHome();
        File toolsJar = new File( javaHome, "../lib/tools.jar" );
        if ( !toolsJar.exists() )
        {
            toolsJar = new File( javaHome, "lib/tools.jar" );
        }
        cp.append( File.pathSeparator );
        cp.append( toolsJar.getAbsolutePath() );

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "getInvokerCP():\n"
                            + "    endorsed: " + toString( endorsedArtifacts ) + "\n"
                            + "    classpath: " + toString( artifactsMap.values() ) + "\n"
                            + "    ecp: " + ecp + "\n"
                            + "    cp: " + cp + "\n"
                            + "    invokerPath: " + invokerPath );
        }

        return new InvokerCP( ecp.toString(), cp.toString(), invokerPath );
    }

    private static class InvokerCP
    {
        public final String ecp;

        public final String cp;

        public final String invokerPath;

        public InvokerCP( String ecp, String cp, String invokerPath )
        {
            this.ecp = ecp;
            this.cp = cp;
            this.invokerPath = invokerPath;
        }
    }

    private String getJavaExec()
    {
        return isWindows() ? "java.exe" : "java";
    }

    private String getJavaHome()
    {
        // by default, java.home from JDK/JRE running Maven
        String javaHome = System.getProperty( "java.home" );
        if ( getJdkToolchain() != null )
        {
            // JDK toolchain used
            File javaExecutable = new File( getJdkToolchain().findTool( "java" ) ); // ${java.home}/bin/java
            javaHome = javaExecutable.getParentFile().getParent();
        }
        return javaHome;
    }

    private File createPathFile( String cp )
        throws IOException
    {
        File f = File.createTempFile( "jax-ws-mvn-plugin-cp", ".txt" );
        if ( f.exists() && f.isFile() && !f.delete() )
        {
            // this should not happen
            getLog().warn( "cannot remove obsolete classpath setting file: " + f.getAbsolutePath() );
        }
        Properties p = new Properties();
        p.put( "cp", cp.replace( File.separatorChar, '/' ) );
        getLog().debug( "stored classpath: " + cp.replace( File.separatorChar, '/' ) );
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( f );
            p.store( fos, null );
        }
        catch ( IOException ex )
        {
            logger.log( Level.SEVERE, null, ex );
        }
        finally
        {
            closeQuietly( fos  );
        }
        return f;
    }

    private boolean isWindows()
    {
        return Os.isFamily( Os.FAMILY_WINDOWS );
    }

    private boolean containsTools( Set<String> cp )
    {
        return cp.contains( "com.sun.xml.ws:jaxws-tools" ) || cp.contains( "org.glassfish.metro:webservices-tools" )
            || cp.contains( "com.oracle.weblogic:weblogic-server-pom" );
    }

    private StringBuilder getCPasString( Collection<Artifact> artifacts )
    {
        StringBuilder sb = new StringBuilder();
        for ( Artifact a : artifacts )
        {
            if ( sb.length() > 0 )
            {
                sb.append( File.pathSeparator );
            }
            sb.append( a.getFile().getAbsolutePath() );
        }
        return sb;
    }

    private String toString( Collection<Artifact> artifacts )
    {
        StringBuilder sb = new StringBuilder();
        for ( Artifact a : artifacts )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ' ' );
            }
            sb.append( a.getGroupId() );
            sb.append( ':' );
            sb.append( a.getArtifactId() );
            sb.append( ':' );
            sb.append( a.getVersion() );
        }
        return sb.toString();
    }

    /**
     * Places the artifact in either the endorsed artifacts set or the normal
     * artifacts map.  It will only add those in "compile" and "runtime" scope
     * or those that are specifically endorsed.
     * 
     * @param a
     *            artifact to sort
     * @param artifactsMap
     *            normal artifacts map
     * @param endorsedArtifacts
     *            endorsed artifacts set
     */
    private void addArtifactToCp( Artifact a, Map<String, Artifact> artifactsMap, Set<Artifact> endorsedArtifacts )
    {
        if ( isEndorsedArtifact( a ) )
        {
            endorsedArtifacts.add( a );
        }
        else if ( "compile".equals( a.getScope() ) || "runtime".equals( a.getScope() ) )
        {
            artifactsMap.put( a.getGroupId() + ":" + a.getArtifactId(), a );
        }
    }

    private boolean isEndorsedArtifact( Artifact a )
    {
        return ( "jaxws-api".equals( a.getArtifactId() )
                || "jaxb-api".equals( a.getArtifactId() )
                || "saaj-api".equals( a.getArtifactId() )
                || "jsr181-api".equals( a.getArtifactId() )
                || "javax.annotation".equals( a.getArtifactId() )
                || "javax.annotation-api".equals( a.getArtifactId() )
                || "webservices-api".equals( a.getArtifactId() )
                || a.getArtifactId().startsWith( "javax.xml.ws" )
                || a.getArtifactId().startsWith( "javax.xml.bind" ) );
    }

    private Toolchain getJdkToolchain()
    {
        Toolchain tc = null;
        if ( toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
        }
        return tc;
    }

    protected void closeQuietly( Object o )
    {
        if ( ( o != null ) && ( o instanceof Closeable ) )
        {
            try
            {
                ( (Closeable) o ).close();
            }
            catch ( IOException ex )
            {
                // ignore
            }
        }
    }
}
