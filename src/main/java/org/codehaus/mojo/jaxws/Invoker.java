/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
 */
package org.codehaus.mojo.jaxws;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lukas
 */
public final class Invoker
{

    public static void main( String... args )
        throws Exception
    {
        String toolClassname = args[0];

        int idx = 1;
        String cp = args[2];
        if ( "-pathfile".equals( args[1] ) )
        {
            // load cp from properties file
            File pathFile = new File( args[2] );
            pathFile.deleteOnExit();
            Properties p = new Properties();
            p.load( new FileInputStream( pathFile ) );
            cp = p.getProperty( "cp" );
            idx = 3;
        }

        URLClassLoader cl = new URLClassLoader( toUrls( cp ) );

        // save original classloader and java.class.path property
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        String origJcp = System.getProperty( "java.class.path" );

        // set to values for tool invocation
        Thread.currentThread().setContextClassLoader( cl );
        System.setProperty( "java.class.path", cp );
        try
        {
            Class<?> toolClass = cl.loadClass( toolClassname );

            Object tool = toolClass.getConstructor( OutputStream.class ).newInstance( System.out );
            Method runMethod = toolClass.getMethod( "run", String[].class );

            String[] wsargs = new String[args.length - idx];
            System.arraycopy( args, idx, wsargs, 0, args.length - idx );

            System.exit( (Boolean) runMethod.invoke( tool, new Object[] { wsargs } ) ? 0 : 1 );
        }
        catch ( NoSuchMethodException ex )
        {
            logSevere( ex );
        }
        catch ( SecurityException ex )
        {
            logSevere( ex );
        }
        catch ( ClassNotFoundException ex )
        {
            logSevere( ex );
        }
        catch ( InstantiationException ex )
        {
            logSevere( ex );
        }
        catch ( IllegalAccessException ex )
        {
            logSevere( ex );
        }
        catch ( IllegalArgumentException ex )
        {
            logSevere( ex );
        }
        catch ( InvocationTargetException ex )
        {
            Exception rex = new RuntimeException();
            rex.initCause( ex );
            throw ex;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( orig );
            System.setProperty( "java.class.path", origJcp );
        }
    }

    private static void logSevere( Exception ex )
    {
        Logger.getLogger( Invoker.class.getName() ).log( Level.SEVERE, null, ex );
    }

    private static URL[] toUrls( String c )
    {
        List<URL> urls = new ArrayList<URL>();
        for ( String s : c.split( File.pathSeparator ) )
        {
            try
            {
                urls.add( new File( s ).toURI().toURL() );
            }
            catch ( MalformedURLException ex )
            {
                Logger.getLogger( Invoker.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
        return urls.toArray( new URL[0] );
    }
}
