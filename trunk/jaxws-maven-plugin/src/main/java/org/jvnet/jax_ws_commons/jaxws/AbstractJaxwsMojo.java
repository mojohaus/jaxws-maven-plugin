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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;

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
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @since 2.3.1
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @since 2.3.1
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project
     * dependencies.
     *
     * @since 2.3.1
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;

    /**
     * The project's remote repositories to use for the resolution of plugins
     * and their dependencies.
     *
     * @since 2.3.1
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> pluginRepos;

    /*
     * Information about this plugin, used to lookup this plugin's configuration
     * from the currently executing project.
     *
     * @since 2.3.1
     */
    @Parameter(defaultValue = "${plugin}", readonly = true)
    protected PluginDescriptor pluginDescriptor;

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
        //try Metro first
        Artifact a = pluginDescriptor.getArtifactMap().get("org.glassfish.metro:webservices-tools");
        List<String> supportedArgs = null;
        String v = null;
        try {
            if (a != null) {
                ArtifactVersion av = a.getSelectedVersion();
                v = av.toString();
                if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() == 0) {
                    supportedArgs = METRO_22;
                } else if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() >= 1) {
                    supportedArgs = METRO_221;
                } else { //if (av.getMajorVersion() >= 2 && av.getMinorVersion() >= 3) {
                    supportedArgs = METRO_23;
                }
            } else {
                //fallback to RI
                a = pluginDescriptor.getArtifactMap().get("com.sun.xml.ws:jaxws-tools");
                ArtifactVersion av = a.getSelectedVersion();
                v = av.toString();
                if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() == 6) {
                    supportedArgs = METRO_22;
                } else if (av.getMajorVersion() == 2 && av.getMinorVersion() == 2 && av.getIncrementalVersion() == 7) {
                    supportedArgs = METRO_221;
                } else { //if (av.getMajorVersion() >= 2 && av.getMinorVersion() >= 2 && av.getIncrementalVersion() >= 8) {
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
                String cp = getExtraClasspath() != null ? getExtraClasspath() + File.pathSeparator : "";
                cp += classpath[1];
                try {
                    File pathFile = createPathFile(cp);
                    cmd.createArg().setLine("-pathfile " + pathFile.getAbsolutePath());
                    if (getExtraClasspath() != null) {
                        cmd.createArg().setValue("-cp");
                        cmd.createArg().setValue(getExtraClasspath());
                    }
                } catch (IOException ioe) {
                    //creation of temporary file can fail, in such case just put everything on cp
                    cmd.createArg().setValue("-cp");
                    cmd.createArg().setValue(cp);
                }
            }
            cmd.setWorkingDirectory(project.getBasedir());
            for (String arg : args) {
                cmd.createArg().setLine(arg);
            }
            String fullCommand = cmd.toString();
            if (isWindows() && 8191 <= fullCommand.length()) {
                getLog().warn("Length of the command is limitted to 8191 characters but it has "
                        + fullCommand.length() + " characters.");
                getLog().warn(fullCommand);
            } else {
                getLog().debug(fullCommand);
            }
            if (CommandLineUtils.executeCommandLine(cmd, sc, sc) != 0) {
                throw new MojoExecutionException("Mojo failed - check output");
            }
        } catch (DependencyResolutionException dre) {
            throw new MojoExecutionException(dre.getMessage(), dre);
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

    private String[] getCP() throws DependencyResolutionException {
        Set<org.sonatype.aether.artifact.Artifact> endorsedCp = new HashSet<org.sonatype.aether.artifact.Artifact>();
        Set<org.sonatype.aether.artifact.Artifact> cp = new HashSet<org.sonatype.aether.artifact.Artifact>();
        Plugin p = pluginDescriptor.getPlugin();
        boolean toolsAdded = false;
        for (Dependency d : p.getDependencies()) {
            if (("jaxws-tools".equals(d.getArtifactId()) && "com.sun.xml.ws".equals(d.getGroupId()))
                    || ("webservices-tools".equals(d.getArtifactId())) && "org.glassfish.metro".equals(d.getGroupId())) {
                toolsAdded = true;
            }
            DependencyResult result = DependencyResolver.resolve(d, pluginRepos, repoSystem, repoSession);
            sortArtifacts(result, cp, endorsedCp);
        }
        if (!toolsAdded) {
            DependencyResult result = DependencyResolver.resolve(
                    pluginDescriptor.getArtifactMap().get("com.sun.xml.ws:jaxws-tools"),
                    pluginRepos, repoSystem, repoSession);
            sortArtifacts(result, cp, endorsedCp);
        }
        StringBuilder sb = getCPasString(cp);
        StringBuilder esb = getCPasString(endorsedCp);
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
        return isWindows() ? "java.exe" : "java";
    }

    private File createPathFile(String cp) throws IOException {
        File f = File.createTempFile("jax-ws-mvn-plugin-cp", ".txt");
        if (f.exists() && f.isFile()) {
            if (!f.delete()) {
                //this should not happen
                getLog().warn("cannot remove obsolete classpath setting file: " + f.getAbsolutePath());
            }
        }
        Properties p = new Properties();
        p.put("cp", cp.replace(File.separatorChar, '/'));
        getLog().debug("stored classpath: " + cp.replace(File.separatorChar, '/'));
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

    private boolean isWindows() {
        return Os.isFamily(Os.FAMILY_WINDOWS);
    }

    private StringBuilder getCPasString(Set<org.sonatype.aether.artifact.Artifact> artifacts) {
        StringBuilder sb = new StringBuilder();
        for (org.sonatype.aether.artifact.Artifact a : artifacts) {
            sb.append(a.getFile().getAbsolutePath());
            sb.append(File.pathSeparator);
        }
        return sb;
    }

    private void sortArtifacts(DependencyResult result, Set<org.sonatype.aether.artifact.Artifact> cp, Set<org.sonatype.aether.artifact.Artifact> endorsedCp) {
        ClassPathNodeListGenerator nlg = new ClassPathNodeListGenerator();
        result.getRoot().accept(nlg);
        cp.addAll(nlg.getArtifacts(false));
        nlg.setEndorsed(true);
        endorsedCp.addAll(nlg.getArtifacts(false));
    }
}
