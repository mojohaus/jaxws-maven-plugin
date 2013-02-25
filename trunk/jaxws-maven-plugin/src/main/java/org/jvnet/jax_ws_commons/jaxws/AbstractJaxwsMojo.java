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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
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
     * Turn off compilation after code generation and let generated sources be
     * compiled by maven during compilation phase; keep is turned on with this option.
     */
    @Parameter(defaultValue = "true")
    private boolean xnocompile;

    /**
     * Map of of plugin artifacts.
     */
    @Parameter(property = "plugin.artifactMap", readonly = true)
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Resolves the artifacts needed.
     *
     * @since 2.3
     */
    @Component
    protected ArtifactResolver artifactResolver;

    /**
     * Creates the artifact.
     *
     * @since 2.3
     */
    @Component
    protected ArtifactFactory artifactFactory;

    /**
     * ArtifactRepository of the localRepository.
     *
     * @since 2.3
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    protected ArtifactRepository localRepository;

    /**
     * The remote plugin repositories declared in the POM.
     *
     * @since 2.3
     */
    @Parameter(defaultValue = "${project.pluginArtifactRepositories}")
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     *
     * @since 2.3
     */
    @Component
    protected ArtifactMetadataSource metadataSource;

    /*
     * Information about this plugin, used to lookup this plugin's configuration from the currently executing
     * project.
     *
     * @since 2.3
     */
    //requires M3 ?
//    @Parameter( defaultValue = "${plugin}", readonly = true )
//    protected PluginDescriptor pluginDescriptor;

    protected abstract String getMain();

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    protected abstract File getDestDir();

    protected abstract File getSourceDestDir();

    protected abstract void addSourceRoot(String sourceDir);

    protected abstract File getDefaultSrcOut();

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
        Artifact a = pluginArtifactMap.get("com.sun.xml.ws:jaxws-tools");
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

    private boolean isDefaultSrc(File srcout) {
        return srcout.equals(getDefaultSrcOut());
    }

    protected void exec(List<String> args) throws MojoExecutionException {
        StreamConsumer sc = new DefaultConsumer();
        try {
            Commandline cmd = new Commandline();
            cmd.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), getJavaExec()).getAbsolutePath());
            cmd.setWorkingDirectory(project.getBasedir());
            String[] classpath = getCP();
            cmd.createArg().setLine("-Xbootclasspath/p:" + classpath[0]);
            cmd.createArg().setLine("-Dcom.sun.tools.xjc.Options.findServices=true");
//            cmd.createArg().setLine("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=6000");
            cmd.createArg().setLine("-cp " + project.getBuild().getOutputDirectory() + File.pathSeparator + classpath[1]);
            cmd.createArg().setLine("org.jvnet.jax_ws_commons.jaxws.Invoker");
            cmd.createArg().setLine(getMain());
            for (String arg : args) {
                cmd.createArg().setLine(arg);
            }
            getLog().debug(cmd.toString());
            CommandLineUtils.executeCommandLine(cmd, sc, sc);
        } catch (Throwable t) {
            throw new MojoExecutionException(t.getMessage(), t);
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
                        getLog().info("excluding: " + e.getGroupId() + ":" + e.getArtifactId());
                    }
                    if (("jaxws-tools".equals(p.getArtifactId()) && "com.sun.xml.ws".equals(p.getGroupId()))
                            || ("webservices-tools".equals(p.getArtifactId())) && "org.glassfish.metro".equals(p.getGroupId())) {
                        toolsAdded = true;
                    }
                    toInclude.add(pluginArtifactMap.get(d.getGroupId() + ":" + d.getArtifactId()));
                    ArtifactFilter filter = new ExcludesArtifactFilter(toExclude);
                    getLog().info("resolving: " + d.getGroupId() + ":" + d.getArtifactId());
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
        sb.append(AbstractJaxwsMojo.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
        sb.append(File.pathSeparator);
        //don't forget tools.jar
        File toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(System.getProperty("java.home"), "lib/tools.jar");
        }
        sb.append(toolsJar.getAbsolutePath());
        sb.append(File.pathSeparator);
        return new String[]{esb.substring(0, esb.length() - 1), sb.substring(0, sb.length() - 1)};
    }

    private String getJavaExec() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "java.exe" : "java";
    }
}
