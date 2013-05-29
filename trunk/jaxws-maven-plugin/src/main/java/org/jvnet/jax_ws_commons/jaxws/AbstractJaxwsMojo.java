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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 *
 * @author dantran <dantran@apache.org>
 * @version $Id: AbstractJaxwsMojo.java 3240 2007-02-04 07:13:21Z dantran $ *
 */
abstract class AbstractJaxwsMojo extends AbstractMojo {

    /**
     * The Maven Project Object.
     */
    @Component
    protected MavenProject project;

    /**
     * Output messages about what the tool is doing.
     */
    @Parameter(defaultValue = "false")
    protected boolean verbose;

    /**
     * Keep generated files.
     */
    @Parameter(defaultValue = "true")
    protected boolean keep;

    /**
     * Allow to use the JAXWS Vendor Extensions.
     */
    @Parameter(defaultValue = "false")
    protected boolean extension;

    /**
     * Specify character encoding used by source files.
     */
    @Parameter(property = "project.build.sourceEncoding")
    private String encoding;

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
     * Path to the executable. Should be either wsgen or wsimport
     * but basically any script which will understand passed in arguments
     * will work.
     *
     * @since 2.2.1
     */
    @Parameter
    private File executable;

    /**
     * Map of of plugin artifacts.
     */
    @Parameter(property = "plugin.artifactMap", readonly = true)
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Resolves the artifacts needed.
     *
     * @since 2.2.1
     */
    @Component
    private ArtifactResolver artifactResolver;

    /**
     * Creates the artifact.
     *
     * @since 2.2.1
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     * ArtifactRepository of the localRepository.
     *
     * @since 2.2.1
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    /**
     * The remote plugin repositories declared in the POM.
     *
     * @since 2.2.1
     */
    @Parameter(defaultValue = "${project.pluginArtifactRepositories}")
    private List<ArtifactRepository> remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     *
     * @since 2.2.1
     */
    @Component
    private ArtifactMetadataSource metadataSource;

    /*
     * Information about this plugin, used to lookup this plugin's configuration from the currently executing
     * project.
     *
     * @since 2.2.1
     */
    //requires M3 ?
//    @Parameter( defaultValue = "${plugin}", readonly = true )
//    protected PluginDescriptor pluginDescriptor;

    private static final Logger logger = Logger.getLogger(AbstractJaxwsMojo.class.getName());
    private static final List<String> METRO_22 = new ArrayList<String>();
    private static final List<String> METRO_221 = new ArrayList<String>();
    private static final List<String> METRO_23 = new ArrayList<String>();

    static {
        METRO_22.add("-encoding");
        METRO_22.add("-clientjar");
        METRO_22.add("-generateJWS");
        METRO_22.add("-implDestDir");
        METRO_22.add("-implServiceName");
        METRO_22.add("-implPortName");
        METRO_221.addAll(METRO_22);
        METRO_221.add("-XdisableAuthenticator");
        METRO_23.addAll(METRO_221);
        METRO_23.add("-x");
    }

    protected abstract String getMain();

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    protected abstract File getDestDir();

    protected abstract File getSourceDestDir();

    protected void addSourceRoot(String sourceDir) {
        if (!project.getCompileSourceRoots().contains(sourceDir)) {
            getLog().debug("adding src root: " + sourceDir);
            project.addCompileSourceRoot(sourceDir);
        } else {
            getLog().debug("existing src root: " + sourceDir);
        }
    }

    protected abstract File getDefaultSrcOut();

    protected abstract boolean getXnocompile();

    protected String getExtraClasspath() {
        return null;
    }

    protected List<String> getCommonArgs() throws MojoExecutionException {
        List<String> commonArgs = new ArrayList<String>();

        if (!isDefaultSrc(getSourceDestDir()) || keep) {
            commonArgs.add("-keep");
            commonArgs.add("-s");
            commonArgs.add(getSourceDestDir().getAbsolutePath());
            if (!getSourceDestDir().mkdirs() && !getSourceDestDir().exists()) {
                getLog().warn("Cannot create directory: " + getSourceDestDir().getAbsolutePath());
            }
            addSourceRoot(getSourceDestDir().getAbsolutePath());
        }

        File destDir = getDestDir();
        if (!destDir.mkdirs() && !destDir.exists()) {
            getLog().warn("Cannot create directory: " + destDir.getAbsolutePath());
        }
        commonArgs.add("-d");
        commonArgs.add(destDir.getAbsolutePath());

        if (verbose) {
            commonArgs.add("-verbose");
        }

        if (isArgSupported("-encoding")) {
            if (encoding != null) {
                maybeUnsupportedOption("-encoding", encoding, commonArgs);
            } else {
                getLog().warn("Using platform encoding (" + System.getProperty("file.encoding") + "), build is platform dependent!");
            }
        }

        if (extension) {
            commonArgs.add("-extension");
        }

        if(getXnocompile()){
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
        Artifact a = pluginArtifactMap.get("com.sun.xml.ws:jaxws-tools");
        List<String> supportedArgs = null;
        String v = null;
        try {
            if (a != null) {
                ArtifactVersion av = a.getSelectedVersion();
                v = av.toString();
                if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() == 6) {
                    supportedArgs = METRO_22;
                } else if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() == 7) {
                    supportedArgs = METRO_221;
                } else { //if (av.getMajorVersion() >= 2 && av.getMinorVersion() >= 2 && av.getIncrementalVersion() >= 8) {
                    supportedArgs = METRO_23;
                }
            } else {
                a = pluginArtifactMap.get("org.glassfish.metro:webservices-tools");
                ArtifactVersion av = a.getSelectedVersion();
                v = av.toString();
                if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() == 0) {
                    supportedArgs = METRO_22;
                } else if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() >= 1) {
                    supportedArgs = METRO_221;
                } else { //if (av.getMajorVersion() >= 2 && av.getMinorVersion() >= 3) {
                    supportedArgs = METRO_23;
                }
            }
        } catch (OverConstrainedVersionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        isSupported = supportedArgs.contains(arg);
        if (!isSupported) {
            getLog().warn("'" + arg + "' is not supported by " + a.getArtifactId() + ":" + v);
        }
        return isSupported;
    }

    private boolean isDefaultSrc(File srcout) {
        return srcout.equals(getDefaultSrcOut());
    }

    protected void exec(List<String> args) throws MojoExecutionException {
        StreamConsumer sc = new DefaultConsumer();
        try {
            Commandline cmd = new Commandline();
            if (executable != null) {
                if (executable.isFile() && executable.canExecute()) {
                    cmd.setExecutable(executable.getAbsolutePath());
                    if (getExtraClasspath() !=  null) {
                        cmd.createArg().setLine("-cp " + getExtraClasspath());
                    }
                } else {
                    throw new MojoExecutionException("Cannot execute: " + executable.getAbsolutePath());
                }
            } else {
                cmd.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), getJavaExec()).getAbsolutePath());
                // add additional JVM options
                if (vmArgs != null) {
                    for (String arg : vmArgs) {
                        cmd.createArg().setLine(arg);
                    }
                }
                String[] classpath = getCP();
                cmd.createArg().setValue("-Xbootclasspath/p:" + classpath[0]);
                cmd.createArg().setValue("-cp");
                cmd.createArg().setValue(classpath[2]);
                cmd.createArg().setLine("org.jvnet.jax_ws_commons.jaxws.Invoker");
                cmd.createArg().setLine(getMain());
                File pathFile = createPathFile(
                        (getExtraClasspath() != null ? getExtraClasspath() + File.pathSeparator : "")
                        + classpath[1]);
                cmd.createArg().setLine("-pathfile " + pathFile.getAbsolutePath());
                if (getExtraClasspath() != null) {
                    cmd.createArg().setLine("-cp " + getExtraClasspath());
                }
            }
            cmd.setWorkingDirectory(project.getBasedir());
            for (String arg : args) {
                cmd.createArg().setLine(arg);
            }
            getLog().debug(cmd.toString());
            if (CommandLineUtils.executeCommandLine(cmd, sc, sc) != 0) {
                throw new MojoExecutionException("Mojo failed - check output");
            }
        } catch (ArtifactNotFoundException t) {
            throw new MojoExecutionException(t.getMessage(), t);
        } catch (ArtifactResolutionException t) {
            throw new MojoExecutionException(t.getMessage(), t);
        } catch (CommandLineException t) {
            throw new MojoExecutionException(t.getMessage(), t);
        }
    }

    protected void maybeUnsupportedOption(String option, String value, List<String> args) {
        if (executable == null) {
            args.add(option);
            if (value != null) {
                args.add(value);
            }
        } else {
            getLog().warn(option + " may not supported on older JDKs.\n"
                    + "Use <args> to bypass this warning if you really want to use it.");
        }
    }

    @SuppressWarnings("unchecked")
    private String[] getCP() throws ArtifactResolutionException, ArtifactNotFoundException {
        Set<Artifact> cp = new HashSet<Artifact>();
        Artifact originatingArtifact = artifactFactory.createBuildArtifact("dummy", "dummy", "1.0", "jar");
        List<Plugin> plugins = project.getBuildPlugins();
        for (Plugin p : plugins) {
            if ("jaxws-maven-plugin".equals(p.getArtifactId()) && "org.jvnet.jax-ws-commons".equals(p.getGroupId())) {
                boolean toolsAdded = false;
                for (Dependency d : p.getDependencies()) {
                    Set<Artifact> toInclude = new HashSet<Artifact>();
                    List<String> toExclude = new ArrayList<String>();
                    for (Exclusion e : d.getExclusions()) {
                        toExclude.add(e.getGroupId() + ":" + e.getArtifactId());
                        getLog().debug("excluding: " + e.getGroupId() + ":" + e.getArtifactId());
                    }
                    if (("jaxws-tools".equals(p.getArtifactId()) && "com.sun.xml.ws".equals(p.getGroupId()))
                            || ("webservices-tools".equals(p.getArtifactId())) && "org.glassfish.metro".equals(p.getGroupId())) {
                        toolsAdded = true;
                    }
                    toInclude.add(pluginArtifactMap.get(d.getGroupId() + ":" + d.getArtifactId()));
                    ArtifactFilter filter = new ExcludesArtifactFilter(toExclude);
                    getLog().debug("resolving: " + d.getGroupId() + ":" + d.getArtifactId());
                    ArtifactResolutionResult res = artifactResolver.resolveTransitively(
                            toInclude,
                            originatingArtifact, localRepository, remoteRepositories,
                            metadataSource, filter);
                    cp.addAll(res.getArtifacts());
                }
                if (!toolsAdded) {
                    ArtifactResolutionResult res = artifactResolver.resolveTransitively(
                            Collections.singleton(pluginArtifactMap.get("com.sun.xml.ws:jaxws-tools")),
                            originatingArtifact, remoteRepositories, localRepository, metadataSource);
                    cp.addAll(res.getArtifacts());
                }
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder esb = new StringBuilder();
        for (Artifact a : cp) {
            if ("jaxws-api".equals(a.getArtifactId()) || "jaxb-api".equals(a.getArtifactId())
                    || "saaj-api".equals(a.getArtifactId()) || "jsr181-api".equals(a.getArtifactId())
                    || "javax.annotation".equals(a.getArtifactId())) {
                esb.append(a.getFile().getAbsolutePath());
                esb.append(File.pathSeparator);
            } else {
                sb.append(a.getFile().getAbsolutePath());
                sb.append(File.pathSeparator);
            }
        }
        //add custom invoker
        String invokerPath = AbstractJaxwsMojo.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
        try {
            invokerPath = new URI(invokerPath.substring(5)).getPath();
            sb.append(invokerPath);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        sb.append(File.pathSeparator);
        //don't forget tools.jar
        File toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(System.getProperty("java.home"), "lib/tools.jar");
        }
        sb.append(toolsJar.getAbsolutePath());
        sb.append(File.pathSeparator);
        return new String[]{esb.substring(0, esb.length() - 1), sb.substring(0, sb.length() - 1), invokerPath};
    }

    private String getJavaExec() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "java.exe" : "java";
    }

    private File createPathFile(String cp) {
        File f = new File(System.getProperty("java.io.tmpdir"), "jm.txt");
        if (f.exists() && f.isFile()) {
            if (!f.delete()) {
                getLog().warn("cannot remove obsolete classpath setting file: " + f.getAbsolutePath());
            }
        }
        Properties p = new Properties();
        p.put("cp", cp.replace(File.separatorChar, '/'));
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            p.store(fos, null);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
        return f;
    }
}
