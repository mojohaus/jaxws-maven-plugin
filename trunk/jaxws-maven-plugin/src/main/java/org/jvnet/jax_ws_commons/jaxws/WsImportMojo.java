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

package org.jvnet.jax_ws_commons.jaxws;

import com.sun.tools.ws.WsImport;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
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

    private static final String PATTERN = "[^\\s]+\\.wsdl$";

    /**
     * The package in which the source files will be generated.
     */
    @Parameter
    private String packageName;

    /**
     * Catalog file to resolve external entity references support TR9401, 
     * XCatalog, and OASIS XML Catalog format.
     */
    @Parameter
    private File catalog;

    /**
     * Set HTTP/HTTPS proxy. Format is <code>[user[:password]@]proxyHost[:proxyPort]</code>.
     */
    @Parameter
    private String httpproxy;

    /**
     * Directory containing WSDL files.
     */
    @Parameter(defaultValue = "${basedir}/src/wsdl")
    private File wsdlDirectory;

    /**
     * List of files to use for WSDLs. If not specified, all <code>.wsdl</code>
     * files in the <code>wsdlDirectory</code> will be used.
     */
    @Parameter
    protected List<String> wsdlFiles;

    /**
     * List of external WSDL URLs to be compiled.
     */
    @Parameter
    private List wsdlUrls;

    /**
     * Directory containing binding files.
     */
    @Parameter(defaultValue = "${basedir}/src/jaxws")
    protected File bindingDirectory;

    /**
     * List of files to use for bindings. If not specified, all <code>.xml</code>
     * files in the <code>bindingDirectory</code> will be used.
     */
    @Parameter
    protected List bindingFiles;

    /**
     * &#64;WebService.wsdlLocation and &#64;WebServiceClient.wsdlLocation value.
     * 
     * <p>
     * Can end with asterisk in which case relative path of the WSDL will
     * be appended to the given <code>wsdlLocation</code>.
     * </p>
     *
     * <p>Example:
     * <pre>
     *  ...
     *  &lt;configuration>
     *      &lt;wsdlDirectory>src/mywsdls&lt;/wsdlDirectory>
     *      &lt;wsdlFiles>
     *          &lt;wsdlFile>a.wsdl&lt;/wsdlFile>
     *          &lt;wsdlFile>b/b.wsdl&lt;/wsdlFile>
     *          &lt;wsdlFile>${basedir}/src/mywsdls/c.wsdl&lt;/wsdlFile>
     *      &lt;/wsdlFiles>
     *      &lt;wsdlLocation>http://example.com/mywebservices/*&lt;/wsdlLocation>
     *  &lt;/configuration>
     *  ...
     * </pre>
     * wsdlLocation for <code>a.wsdl</code> will be http://example.com/mywebservices/a.wsdl<br/>
     * wsdlLocation for <code>b/b.wsdl</code> will be http://example.com/mywebservices/b/b.wsdl<br/>
     * wsdlLocation for <code>${basedir}/src/mywsdls/c.wsdl</code> will be file://absolute/path/to/c.wsdl
     * </p>
     *
     * <p>
     * Note: External binding files cannot be used if asterisk notation is in place.
     * </p>
     */
    @Parameter
    private String wsdlLocation;

    /**
     * Generate code as per the given JAXWS specification version.
     * Setting "2.0" will cause JAX-WS to generate artifacts
     * that run with JAX-WS 2.0 runtime.
     */
    @Parameter
    private String target;

    /**
     * Suppress wsimport output.
     */
    @Parameter(defaultValue = "false")
    private boolean quiet;

    /**
     * Local portion of service name for generated JWS implementation.
     * Implies <code>genJWS=true</code>.
     *
     * Note: It is a QName string, formatted as: "{" + Namespace URI + "}" + local part
     */
    @Parameter
    private String implServiceName = null;

    /**
     * Local portion of port name for generated JWS implementation.
     * Implies <code>genJWS=true</code>.
     *
     * Note: It is a QName string, formatted as: "{" + Namespace URI + "}" + local part
     */
    @Parameter
    private String implPortName = null;

    /**
     * Generate stubbed JWS implementation file.
     */
    @Parameter(defaultValue = "false")
    private boolean genJWS;

    /**
     * Maps headers not bound to the request or response messages to Java method parameters.
     */
    @Parameter(defaultValue = "false")
    private boolean xadditionalHeaders;

    /**
     * Turn on debug message.
     */
    @Parameter(defaultValue = "false")
    private boolean xdebug;

    /**
     * Binding W3C EndpointReferenceType to Java. By default WsImport follows spec and does not bind
     * EndpointReferenceType to Java and uses the spec provided {@link javax.xml.ws.wsaddressing.W3CEndpointReference}
     */
    @Parameter(defaultValue = "false")
    private boolean xnoAddressingDataBinding;

    /**
     * Specify the location of authorization file.
     */
    @Parameter
    protected File xauthFile;

    /**
     * Disable the SSL Hostname verification while fetching WSDL(s).
     */
    @Parameter(defaultValue = "false")
    private boolean xdisableSSLHostnameVerification;

    /**
     */
    @Parameter(defaultValue = "false")
    private boolean xuseBaseResourceAndURLToLoadWSDL;

    /**
     * Disable Authenticator used by JAX-WS RI, <code>xauthfile</code> will be ignored if set.
     */
    @Parameter(defaultValue = "false")
    private boolean xdisableAuthenticator;

    /**
     * Specify optional XJC-specific parameters that should simply be passed to <code>xjc</code>
     * using <code>-B</code> option of WsImport command.
     * <p>
     * Multiple elements can be specified, and each token must be placed in its own list.
     * </p>
     */
    @Parameter
    private List<String> xjcArgs;

    /**
     * The folder containing flag files used to determine if the output is stale.
     */
    @Parameter(defaultValue = "${project.build.directory}/jaxws/stale")
    private File staleFile;

    /**
     */
    @Parameter(property = "settings", readonly = true)
    private Settings settings;

    protected abstract File getImplDestDir();

    @Override
    public void execute()
        throws MojoExecutionException
    {

        // Need to build a URLClassloader since Maven removed it form the chain
        ClassLoader parent = this.getClass().getClassLoader();
        String originalSystemClasspath = this.initClassLoader( parent );

        try
        {

            URL[] wsdls = getWSDLFiles();
            if (wsdls.length == 0 && (wsdlUrls == null || wsdlUrls.isEmpty())) {
                getLog().info( "No WSDLs are found to process, Specify atleast one of the following parameters: wsdlFiles, wsdlDirectory or wsdlUrls.");
                return;
            }

            this.processWsdlViaUrls();

            this.processLocalWsdlFiles(wsdls);
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
    private void processLocalWsdlFiles(URL[] wsdls)
        throws MojoExecutionException,  IOException
    {
        for (URL u : wsdls) {
            String url = u.toExternalForm();
            if (isOutputStale(url)) {
                getLog().info("Processing: " + url);
                String relPath = null;
                if ("file".equals(u.getProtocol())) {
                    relPath = getRelativePath(new File(u.getPath()));
                }
                ArrayList<String> args = getWsImportArgs(relPath);
                args.add(url);
                getLog().info("jaxws:wsimport args: " + args);
                wsImport(args);
                touchStaleFile(url);
            } else {
                getLog().info("Ignoring: " + url);
                //http://java.net/jira/browse/JAX_WS_COMMONS-95
                addSourceRoot(getSourceDestDir().getAbsolutePath());
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
                ArrayList<String> args = getWsImportArgs(null);
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
        if (WsImport.doMain(args.toArray(new String[args.size()])) != 0)
            throw new MojoExecutionException("Error executing: wsimport " + args);
      } catch (Throwable t) {
          throw new MojoExecutionException( "Error executing: wsimport " + args, t);
      }
    }

    /**
     * 
     * @return wsimport's command arguments
     * @throws MojoExecutionException
     */
    private ArrayList<String> getWsImportArgs(String relativePath)
        throws MojoExecutionException
    {
        ArrayList<String> args = new ArrayList<String>();
        args.addAll(getCommonArgs());

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
            if (relativePath != null) {
                args.add("-wsdllocation");
                args.add(wsdlLocation.replaceAll("\\*", relativePath));
            } else if (!wsdlLocation.contains("*")) {
                args.add( "-wsdllocation" );
                args.add(wsdlLocation);
            }
        }

        if ( target != null )
        {
            args.add( "-target" );
            args.add( target );
        }

        if (quiet) {
            args.add("-quiet");
        }

        if (genJWS || implServiceName != null || implPortName != null) {
            args.add("-generateJWS");
            if (implServiceName != null) {
                args.add("-implServiceName");
                args.add(implServiceName);
            }
            if (implPortName != null) {
                args.add("-implPortName");
                args.add(implPortName);
            }
            getImplDestDir().mkdirs();
            args.add("-implDestDir");
            args.add(getImplDestDir().getAbsolutePath());
        }

        if(xdebug){
            args.add("-Xdebug");
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

        if (xdisableSSLHostnameVerification) {
            args.add("-XdisableSSLHostnameVerification");
        }
        if (xuseBaseResourceAndURLToLoadWSDL) {
            args.add("-XuseBaseResourceAndURLToLoadWSDL");
        }
        if (xdisableAuthenticator) {
            args.add("-XdisableAuthenticator");
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
        File[] bindings = getBindingFiles();

        if (bindings.length > 0 && wsdlLocation != null && wsdlLocation.contains("*")) {
            throw new MojoExecutionException("External binding file(s) can not be bound to more WSDL files (" + wsdlLocation + ")\n"
                    + "Please use either inline binding(s) or multiple execution tags.");
        }

        for (File binding : bindings) {
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
    private URL[] getWSDLFiles()
    {
        URL[] files;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if ( wsdlFiles != null )
        {
            files = new URL[ wsdlFiles.size() ];
            for ( int i = 0 ; i < wsdlFiles.size(); ++i )
            {
                String wsdlFileName = (String) wsdlFiles.get( i );
                File wsdl = new File(wsdlFileName);
                if (!wsdl.isAbsolute()) {
                    wsdl = new File(wsdlDirectory, wsdlFileName);
                }
                if (!wsdl.exists()) {
                    files[i] = loader.getResource(wsdlFileName);
                } else {
                    try {
                        files[i] = wsdl.toURI().toURL();
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(WsImportMojo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                getLog().debug( "The wsdl File is '" +  wsdlFileName + "' from '" + files[i] + "'");
            }
        }
        else
        {
            getLog().debug( "The wsdl Directory is " + wsdlDirectory );
            if (wsdlDirectory.exists()) {
                File[] wsdls = wsdlDirectory.listFiles(new WSDLFile());
                files = new URL[wsdls != null ? wsdls.length : 0];
                for (int i = 0; i < files.length; i++) {
                    try {
                        files[i] = wsdls[i].toURI().toURL();
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(WsImportMojo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                URI rel = project.getBasedir().toURI().relativize(wsdlDirectory.toURI());
                String dir = rel.getPath();
                URL u = loader.getResource(dir);
                if (u == null) {
                    dir = "WEB-INF/wsdl/";
                    u = loader.getResource(dir);
                }
                if (u == null) {
                    dir = "META-INF/wsdl/";
                    u = loader.getResource(dir);
                }
                if (u == null || !"jar".equalsIgnoreCase(u.getProtocol())) {
                    files = new URL[0];
                } else {
                    List<URL> res = new ArrayList<URL>();
                    String path = u.getPath();
                    try {
                        Pattern p = Pattern.compile(dir.replace(File.separatorChar, '/') + PATTERN, Pattern.CASE_INSENSITIVE);
                        Enumeration<JarEntry> jes = new JarFile(path.substring(5, path.indexOf("!/"))).entries();
                        while (jes.hasMoreElements()) {
                            JarEntry je = jes.nextElement();
                            Matcher m = p.matcher(je.getName());
                            if (m.matches()) {
                                String s = "jar:" + path.substring(0, path.indexOf("!/") + 2) + je.getName();
                                res.add(new URL(s));
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(WsImportMojo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    files = res.toArray(new URL[res.size()]);
                }
            }
        }
        return files;
    }

    /**
     * A class used to look up .xml documents from a given directory.
     */
    private static final class XMLFile
        implements FileFilter
    {
        /**
         * Returns true if the file ends with an xml extension.
         *
         * @param file
         *            The filed being reviewed by the filter.
         * @return true if an xml file.
         */
        @Override
        public boolean accept( final java.io.File file )
        {
            return file.getName().endsWith( ".xml" );
        }
    }

    /**
     * A class used to look up .wsdl documents from a given directory.
     */
    private static final class WSDLFile
        implements FileFilter
    {

        /**
         * Returns true if the file ends with a wsdl extension.
         *
         * @param file
         *            The filed being reviewed by the filter.
         * @return true if an wsdl file.
         */
        @Override
        public boolean accept( final java.io.File file )
        {
            return file.getName().endsWith( ".wsdl" );
        }

    }

    private String getRelativePath(File f) {
        if (wsdlFiles != null) {
            for (String s : wsdlFiles) {
                String path = f.getPath().replace(File.separatorChar, '/');
                if (path.endsWith(s) && path.length() != s.length()) {
                    return s;
                }
            }
        }
        return null;
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
        return s.substring(s.lastIndexOf('/')).replaceAll("\\.", "-");
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
