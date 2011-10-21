/*
 * Copyright 2006 Guillaume Nodet.
 * Copyright 2007-2011 JAX-WS RI team.
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


import com.sun.tools.ws.Invoker;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;

/**
 * 
 * @author gnodet <gnodet@apache.org>
 * @author dantran <dantran@apache.org>
 * @version $Id: WsImportMojo.java 3169 2007-01-22 02:51:29Z dantran $
 */
abstract class WsImportMojo extends AbstractJaxwsMojo
{

    private static final String STALE_FILE_PREFIX = ".";
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
     * Maps headers not bound to the request or response messages to Java method params.
     *
     * @parameter default-value="false"
     */
    private Boolean xadditionalHeaders;

    /**
     * Turn on debug message
     *
     * @parameter default-value="false"
     */
    private boolean xdebug;

    /**
     * Turn off compilation after code generation and let generated sources be
     * compiled by maven during compilation phase; keep is turned on with this option.
     *
     * @parameter default-value="true"
     */
    private boolean xnocompile;
    
    /**
     * Binding W3C EndpointReferenceType to Java. By default Wsimport follows spec and does not bind
     * EndpointReferenceType to Java and uses the spec provided {@link javax.xml.ws.wsaddressing.W3CEndpointReference}
     *
     * @parameter default-value="false"
     */
    private boolean xnoAddressingDataBinding;

    /**
     * Specify the location of authorization file.
     *
     * @parameter 
     */
    protected File xauthFile;

    /**
     * Specify optional XJC-specific parameters that should simply be passed to xjc
     * using -B option of WsImport command.
     * <p>
     * Multiple elements can be specified, and each token must be placed in its own list.
     * </p>
     * @parameter
     */
    private List<String> xjcArgs;

    /**
     * Specify optional wsimport commnd-line options.
     * <p>
     * Multiple elements can be specified, and each token must be placed in its own list.
     * </p>
     * @parameter
     */
    private List<String> args;
    
    /**
     * The folder containing flag files used to determine if the output is stale.
     *
     * @parameter default-value="${project.build.directory}/jaxws/stale"
     */
    private File staleFile;

    /**
     * @parameter default-value="${settings}"
     * @readonly
     */
    private Settings settings;

    protected abstract File getSourceDestDir();

    protected abstract void addSourceRoot(String sourceDir);

    public void execute()
        throws MojoExecutionException
    {

        // Need to build a URLClassloader since Maven removed it form the chain
        ClassLoader parent = this.getClass().getClassLoader();
        String originalSystemClasspath = this.initClassLoader( parent );

        try
        {

            File[] wsdls = getWSDLFiles();
            if(wsdls.length == 0 && (wsdlUrls == null || wsdlUrls.size() ==0)){
                getLog().info( "No WSDLs are found to process, Specify atleast one of the following parameters: wsdlFiles, wsdlDirectory or wsdlUrls.");
                return;
            }

            getSourceDestDir().mkdirs();
            getDestDir().mkdirs();
            
            this.processWsdlViaUrls();

            this.processLocalWsdlFiles(wsdls);

            // even thou the generated source already compiled, we still want to 
            //  add the source path so that IDE can pick it up
            if (xnocompile) {
                addSourceRoot(getSourceDestDir().getAbsolutePath());
            }

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
    private void processLocalWsdlFiles(File[] wsdls)
        throws MojoExecutionException,  IOException
    {
        for (File wsdl : wsdls) {
            //XXX wouldn't wsdl.getPath() be enough here?
            if (isOutputStale(wsdl.getAbsolutePath())) {
                getLog().info("Processing: " + wsdl.getAbsolutePath());
                ArrayList<String> args = getWsImportArgs();
                args.add(wsdl.getAbsolutePath());
                getLog().info("jaxws:wsimport args: " + args);
                wsImport(args);
                touchStaleFile(wsdl.getAbsolutePath());
            }
        }
    }

    /**
     * process external wsdl
     * @throws MojoExecutionException
     */
    private void processWsdlViaUrls()
        throws MojoExecutionException, IOException
    {
        for (int i = 0; wsdlUrls != null && i < wsdlUrls.size(); i++) {
            String wsdlUrl = wsdlUrls.get(i).toString();
            if (isOutputStale(wsdlUrl)) {
                getLog().info("Processing: " + wsdlUrl);
                ArrayList<String> args = getWsImportArgs();
                args.add(wsdlUrl);
                getLog().info("jaxws:wsimport args: " + args);
                wsImport(args);
                touchStaleFile(wsdlUrl);
            }
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
      try {
        final ClassLoader cl = Invoker.createClassLoader(Thread.currentThread().getContextClassLoader());
        Class c = cl.loadClass("com.sun.tools.ws.wscompile.WsimportTool");
        Object tool = c.getConstructor(OutputStream.class).newInstance(System.out);
        String[] ar = args.toArray( new String[args.size()] );
        Boolean result = (Boolean)c.getMethod("run", ar.getClass()).invoke(tool, new Object[]{ar});
        if ( !result )
        {
            throw new MojoExecutionException( "Error executing: wsimport " + args );
        }
      } catch (Exception e) {
        throw new MojoExecutionException( "Error executing: wsimport " + args, e);
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
        args.add( getSourceDestDir().getAbsolutePath() );

        args.add( "-d" );
        args.add( getDestDir().getAbsolutePath() );

        if ( verbose )
        {
            args.add( "-verbose" );
        }

        if ( httpproxy != null )
        {
            args.add( "-httpproxy:" + httpproxy);
        }
        else if (settings != null)
        {
            String proxyString = getActiveHttpProxy(settings);
            if (proxyString != null)
            {
                args.add( "-httpproxy:" + proxyString);
            }
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

        if(xdebug){
            args.add("-Xdebug");
        }

        if(xnocompile){
            args.add("-Xnocompile");
        }
        
        /**
         * -Xno-addressing-databinding enable binding of W3C EndpointReferenceType to Java
         */
        if(xnoAddressingDataBinding){
            args.add("-Xno-addressing-databinding");
        }

        if(xadditionalHeaders){
            args.add("-XadditionalHeaders");
        }

        if(xauthFile != null){
            args.add("-Xauthfile");
            args.add(xauthFile.getAbsolutePath());
        }

        // add wsimport commandline Options
        if (this.args != null)
        {
            for (String arg : this.args) {
                args.add(arg);
            }
        }

        // xjcOIptions
        if (xjcArgs != null) 
        {
            for (String xjcArg : xjcArgs) {
                if (xjcArg.startsWith("-"))
                    args.add("-B" + xjcArg);
                else
                    args.add(xjcArg);
            }
        }
        
        
        // Bindings
        for (File binding : getBindingFiles()) {
            args.add("-b");
            args.add(binding.getAbsolutePath());
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
                String wsdlFileName = (String) wsdlFiles.get( i );
                File wsdl = new File(wsdlFileName);
                if (!wsdl.isAbsolute()) {
                    wsdl = new File(wsdlDirectory, wsdlFileName);
                }
                getLog().debug( "The wsdl File is " +  wsdlFileName);
                files[i] = wsdl;
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
     * Returns true if given WSDL resource or any binding file is newer
     * than the <code>staleFlag</code> file.
     * 
     * @return True if wsdl files have been modified since the last build.
     */
    private boolean isOutputStale(String resource)
    {
        File[] sourceBindings = getBindingFiles();
        File stFile = new File(staleFile, STALE_FILE_PREFIX + getHash(resource));
        boolean stale = !stFile.exists();
        if ( !stale )
        {
            getLog().debug("Stale flag file exists, comparing to wsdls and bindings.");
            long staleMod = stFile.lastModified();

            try {
                //resource can be URL
                URL sourceWsdl = new URL(resource);
                if (sourceWsdl.openConnection().getLastModified() > staleMod) {
                    getLog().debug(resource + " is newer than the stale flag file.");
                    stale = true;
                }
            } catch (MalformedURLException mue) {
                //or a file
                File sourceWsdl = new File(resource);
                if (sourceWsdl.lastModified() > staleMod) {
                    getLog().debug(resource + " is newer than the stale flag file.");
                    stale = true;
                }
            } catch (IOException ioe) {
                //possible error while openning connection
                getLog().error(ioe);
            }

            for (File sourceBinding : sourceBindings) {
                if (sourceBinding.lastModified() > staleMod) {
                    getLog().debug(sourceBinding.getName() + " is newer than the stale flag file.");
                    stale = true;
                }
            }
        }
        return stale;
    }

    private void touchStaleFile(String resource)
        throws IOException
    {
        File stFile = new File(staleFile, STALE_FILE_PREFIX + getHash(resource));
        if ( !stFile.exists() )
        {
            stFile.getParentFile().mkdirs();
            stFile.createNewFile();
            getLog().debug( "Stale flag file created.[" + stFile.getAbsolutePath() + "]");
        }
        else
        {
            stFile.setLastModified( System.currentTimeMillis() );
        }
    }

    private String getHash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            Formatter formatter = new Formatter();
            for (byte b : md.digest(s.getBytes("UTF-8"))) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (UnsupportedEncodingException ex) {
            getLog().debug(ex.getMessage(), ex);
        } catch (NoSuchAlgorithmException ex) {
            getLog().debug(ex.getMessage(), ex);
        }
        //fallback to some default
        getLog().warn("Could not compute hash for " + s + ". Using fallback method.");
        return s.substring(s.lastIndexOf('/')).replaceAll(".", "-");
    }

    /**
     * 
     * @return proxy string as [user[:password]@]proxyHost[:proxyPort] or null
     */
    static String getActiveHttpProxy(Settings s) {
        String retVal = null;
        for (Proxy p : (List<Proxy>) s.getProxies()) {
            if (p.isActive() && "http".equals(p.getProtocol())) {
                StringBuilder sb = new StringBuilder();
                String user = p.getUsername();
                String pwd = p.getPassword();
                if (user != null) {
                    sb.append(user);
                    if (pwd != null) {
                        sb.append(":");
                        sb.append(pwd);
                    }
                    sb.append("@");
                }
                sb.append(p.getHost());
                sb.append(":");
                sb.append(p.getPort());
                retVal = sb.toString().trim();
                break;
            }
        }
        return retVal;
    }
}
