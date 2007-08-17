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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import com.sun.tools.ws.wscompile.WsimportTool;

/**
 * 
 * @author gnodet <gnodet@apache.org>
 * @author dantran <dantran@apache.org>
 * @version $Id: WsImportMojo.java 3169 2007-01-22 02:51:29Z dantran $
 */
abstract class WsImportMojo extends AbstractJaxwsMojo
{

    /**
     * The package in which the source files will be generated.
     * 
     * @parameter
     */
    private String packageName;

    /**
     * Catalog file to resolve external entity references support TR9401, 
     * XCatalog, and OASIS XML Catalog format.
     * 
     * @parameter
     */
    private File catalog;

    /**
     * Set HTTP/HTTPS proxy. Format is [user[:password]@]proxyHost[:proxyPort]
     * 
     * @parameter
     */
    private String httpproxy;

    /**
     * Directory containing wsdl files.
     * 
     * @parameter default-value="${basedir}/src/wsdl"
     */
    private File wsdlDirectory;

    /**
     * List of files to use for wsdls. If not specified, all .wsdl files 
     * in the wsdlDirectory will be used.
     * 
     * @parameter
     */
    protected List wsdlFiles;

    /**
     * List of external wsdl urls to be compiled.
     * 
     * @parameter
     */
    private List wsdlUrls;

    /**
     * Directory containing binding files.
     * 
     * @parameter default-value="${basedir}/src/jaxws"
     */
    protected File bindingDirectory;

    /**
     * List of files to use for bindings.If not specified, all .xml files 
     * in the bindingDirectory will be used.
     * 
     * @parameter
     */
    protected List bindingFiles;

    /**
     * @WebService.wsdlLocation and
     * @WebServiceClient.wsdlLocation value.
     * 
     * @parameter
     */
    private String wsdlLocation;

    /**
     * Generate code as per the given JAXWS specification version.
     * Setting "2.0" will cause JAX-WS to generate artifacts
     * that run with JAX-WS 2.0 runtime.
     * 
     * @parameter
     */
    private String target;
    
    /**
     * Specify where to place generated source files, keep is turned on with this option. 
     * 
     * @parameter default-value="${project.build.directory}/jaxws/wsimport/java"
     */
    protected File sourceDestDir;
    
    /**
     * Specify optional XJC-specific parameters that should simply be passed to xjc
     * using -B option of WsImport command.
     * 
     * @parameter
     */
    private List xjcArgs;
    
    /**
     * The location of the flag file used to determine if the output is stale.
     * @parameter default-value="${project.build.directory}/jaxws/stale/.staleFlag"
     */
    private File staleFile;

    public void execute()
        throws MojoExecutionException
    {

        // Need to build a URLClassloader since Maven removed it form the chain
        ClassLoader parent = this.getClass().getClassLoader();
        String originalSystemClasspath = this.initClassLoader( parent );

        try
        {

            sourceDestDir.mkdirs();
            getDestDir().mkdirs();

            this.processWsdlViaUrls();

            this.processLocalWsdlFiles();

            // even thou the generated source already compiled, we still want to 
            //  add the source path so that IDE can pick it up
            project.addCompileSourceRoot( sourceDestDir.getAbsolutePath() );

        }
        catch ( MojoExecutionException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            // Set back the old classloader
            Thread.currentThread().setContextClassLoader( parent );
            System.setProperty( "java.class.path", originalSystemClasspath );
        }

    }

    /**
     * 
     * @throws MojoExecutionException
     * @throws IOException
     */
    private void processLocalWsdlFiles()
        throws MojoExecutionException,  IOException
    {

        if ( isOutputStale() )
        {
            File[] wsdls = getWSDLFiles();
            for ( int i = 0; i < wsdls.length; i++ )
            {
                getLog().info( "Processing: " + wsdls[i].getAbsolutePath() );
                ArrayList<String> args = getWsImportArgs();
                args.add( wsdls[i].getAbsolutePath() );
                getLog().info( "jaxws:wsimport args: " + args );                  
                wsImport( args );
            	              
            }
            touchStaleFile();
        }

    }

    /**
     * process external wsdl
     * @throws MojoExecutionException
     */
    private void processWsdlViaUrls()
        throws MojoExecutionException
    {
        //TODO can we do some stale check against a URL?
        
        for ( int i = 0; wsdlUrls != null && i < wsdlUrls.size(); i++ )
        {
            String wsdlUrl = wsdlUrls.get( i ).toString();

            getLog().info( "Processing: " + wsdlUrl );
            ArrayList<String> args = getWsImportArgs();
            args.add( wsdlUrl );
            getLog().info( "jaxws:wsimport args: " + args );      
            wsImport( args );
        }
    }

    /**
     * Invoke wsimport compiler
     * @param args
     * @throws MojoExecutionException
     */
    private void wsImport( ArrayList<String> args )
        throws MojoExecutionException
    {
        WsimportTool compTool = new WsimportTool( System.out );
        if ( !compTool.run( args.toArray( new String[args.size()] ) ) )
        {
            throw new MojoExecutionException( "Error executing: wsimport " + args );
        }
    }

    /**
     * 
     * @return wsimport's command arguments
     * @throws MojoExecutionException
     */
    private ArrayList<String> getWsImportArgs()
        throws MojoExecutionException
    {
        ArrayList<String> args = new ArrayList<String>();

        args.add( "-s" );
        args.add( sourceDestDir.getAbsolutePath() );

        args.add( "-d" );
        args.add( getDestDir().getAbsolutePath() );

        if ( verbose )
        {
            args.add( "-verbose" );
        }

        if ( httpproxy != null )
        {
            args.add( "-httpproxy" );
            args.add( httpproxy );
        }

        if ( packageName != null )
        {
            args.add( "-p" );
            args.add( packageName );
        }

        if ( catalog != null )
        {
            args.add( "-catalog" );
            args.add( catalog.getAbsolutePath() );
        }

        if ( wsdlLocation != null )
        {
            args.add( "-wsdllocation" );
            args.add( wsdlLocation );
        }

        if ( target != null )
        {
            args.add( "-target" );
            args.add( target );
        }
        
        if ( extension )
        {
            args.add( "-extension" );
        }

        // xjcOIptions
        if (xjcArgs != null) 
        {
            Iterator xjcArgsIter = xjcArgs.iterator();
            while ( xjcArgsIter.hasNext() )
            {
                String xjcArg = (String)xjcArgsIter.next();
                args.add( "-B" + xjcArg );
            }
        }
        
        // Bindings
        File bindings[] = getBindingFiles();
        for ( int i = 0; i < bindings.length; i++ )
        {
            args.add( "-b" );
            args.add( bindings[i].getAbsolutePath() );
        }

        getLog().debug( "jaxws:wsimport args: " + args );

        return args;
    }

    /**
     * Returns a file array of xml files to translate to object models.
     * 
     * @return An array of schema files to be parsed by the schema compiler.
     */
    public final File[] getBindingFiles()
    {
        File [] bindings;
        
        if ( bindingFiles != null )
        {
            bindings = new File[bindingFiles.size()];
            for ( int i = 0 ; i < bindingFiles.size(); ++i ) 
            {
                String schemaName = (String) bindingFiles.get( i );
                File file = new File( schemaName );
                if (!file.isAbsolute()) {
                    file = new File( bindingDirectory, schemaName );
                }
                bindings[i] = file;
            }
        }
        else
        {
            getLog().debug( "The binding Directory is " + bindingDirectory );
            bindings =  bindingDirectory.listFiles( new XMLFile() );
            if ( bindings == null )
            {
                bindings = new File[0];
            }
        }

        return bindings;
    }

    /**
     * Returns a file array of wsdl files to translate to object models.
     * 
     * @return An array of schema files to be parsed by the schema compiler.
     */
    public final File[] getWSDLFiles()
    {
        File [] files;

        if ( wsdlFiles != null )
        {
            files = new File[ wsdlFiles.size() ];
            for ( int i = 0 ; i < wsdlFiles.size(); ++i ) 
            {
                String schemaName = (String) wsdlFiles.get( i );
                files[i] = new File( wsdlDirectory, schemaName ) ;
            }
        }
        else
        {
            getLog().debug( "The wsdl Directory is " + wsdlDirectory );
            files = wsdlDirectory.listFiles( new WSDLFile() );
            if ( files == null )
            {
                files = new File[0];
            }
        }

        return files;
    }

    /**
     * A class used to look up .xml documents from a given directory.
     */
    private final class XMLFile
        implements FileFilter
    {
        /**
         * Returns true if the file ends with an xml extension.
         * 
         * @param file
         *            The filed being reviewed by the filter.
         * @return true if an xml file.
         */
        public boolean accept( final java.io.File file )
        {
            return file.getName().endsWith( ".xml" );
        }
    }

    /**
     * A class used to look up .wsdl documents from a given directory.
     */
    private final class WSDLFile
        implements FileFilter
    {

        /**
         * Returns true if the file ends with a wsdl extension.
         * 
         * @param file
         *            The filed being reviewed by the filter.
         * @return true if an wsdl file.
         */
        public boolean accept( final java.io.File file )
        {
            return file.getName().endsWith( ".wsdl" );
        }

    }

    /**
     * Returns true of any one of the files in the WSDL/XJB array are more new than the <code>staleFlag</code> file.
     * 
     * @return True if wsdl files have been modified since the last build.
     */
    private boolean isOutputStale()
    {
        File[] sourceWsdls = getWSDLFiles();
        File[] sourceBindings = getBindingFiles();
        boolean stale = !staleFile.exists();
        if ( !stale )
        {
            getLog().debug( "Stale flag file exists, comparing to wsdls and bindings." );
            long staleMod = staleFile.lastModified();

            for ( int i = 0; i < sourceWsdls.length; i++ )
            {
                if ( sourceWsdls[i].lastModified() > staleMod )
                {
                    getLog().debug( sourceWsdls[i].getName() + " is newer than the stale flag file." );
                    stale = true;
                }
            }

            for ( int i = 0; i < sourceBindings.length; i++ )
            {
                if ( sourceBindings[i].lastModified() > staleMod )
                {
                    getLog().debug( sourceBindings[i].getName() + " is newer than the stale flag file." );
                    stale = true;
                }
            }
        }
        return stale;
    }

    private void touchStaleFile()
        throws IOException
    {
        if ( !staleFile.exists() )
        {
            staleFile.getParentFile().mkdirs();
            staleFile.createNewFile();
            getLog().debug( "Stale flag file created." );
        }
        else
        {
            staleFile.setLastModified( System.currentTimeMillis() );
        }
    }

}
