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

package org.codehaus.mojo.jaxws;

import com.sun.tools.ws.WsGen;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;

/**
 * <p>
 * A Maven 2 plugin which reads a service endpoint implementation class 
 *   and generates all of the portable artifacts for a JAX-WS web service.
 * </p>
 * 
 * @goal wsgen
 * @phase generate-sources
 * @requiresDependencyResolution
 * @description JAXWS 2.x Plugin.
 * @author gnodet <gnodet@apache.org>
 * @author dantran <dantran@apache.org>
 * @version $Id: WsGenMojo.java 3169 2007-01-22 02:51:29Z dantran $
 */
public class WsGenMojo
    extends AbstractJaxwsMojo
{

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
     * @required
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
     * Specify where to place generated source files, keep is turned on with this option. 
     * 
     * @parameter 
     */
    private File sourceDestDir;
    //default-value="${project.build.directory}/jaxws/java"

    public void execute()
        throws MojoExecutionException, MojoFailureException {
        init();

        // Need to build a URLClassloader since Maven removed it form the chain
        ClassLoader parent = this.getClass().getClassLoader();
        String orginalSystemClasspath = this.initClassLoader(parent);

        try {
            ArrayList<String> args = getWsGenArgs();

            if (WsGen.doMain(args.toArray(new String[args.size()])) != 0)
                throw new MojoExecutionException("Error executing: wsgen " + args);
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

    private void init()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !destDir.exists() )
        {
            destDir.mkdirs();
        }
    }

    /**
     * Construct wsgen arguments
     * @return a list of arguments
     * @throws MojoExecutionException
     */
    private ArrayList<String> getWsGenArgs()
        throws MojoExecutionException {
        ArrayList<String> args = new ArrayList<String>();

        if (verbose) {
            args.add("-verbose");
        }

        if (keep) {
            args.add("-keep");
        }

        if (this.sourceDestDir != null) {
            args.add("-s");
            args.add(this.sourceDestDir.getAbsolutePath());
            this.sourceDestDir.mkdirs();
        }

        args.add("-d");
        args.add(this.destDir.getAbsolutePath());
        this.destDir.mkdirs();

        if (this.genWsdl) {
            if (this.protocol != null) {
                args.add("-wsdl:" + this.protocol);
            } else {
                args.add("-wsdl");
            }

            args.add("-r");
            args.add(this.resourceDestDir.getAbsolutePath());
            this.resourceDestDir.mkdirs();

        }

        args.add(sei);

        getLog().info("jaxws:wsgen args: " + args);

        return args;
    }

}
