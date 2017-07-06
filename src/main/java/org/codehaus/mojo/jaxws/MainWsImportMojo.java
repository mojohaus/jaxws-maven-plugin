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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Parses wsdl and binding files and generates Java code needed to access it.
 *
 * @author Kohsuke Kawaguchi
 */
@Mojo( name = "wsimport", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true )
public class MainWsImportMojo
    extends WsImportMojo
{

    /**
     * Specify where to place output generated classes. Use <code>xnocompile</code>
     * to turn this off.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private File destDir;

    /**
     * Specify where to place generated source files, keep is turned on with this option.
     */
    @Parameter( defaultValue = "${project.build.directory}/generated-sources/wsimport" )
    private File sourceDestDir;

    /**
     * Specify where to generate JWS implementation file.
     */
    @Parameter( defaultValue = "${project.build.sourceDirectory}" )
    private File implDestDir;

    /**
     * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
     */
    @Override
    protected File getDestDir()
    {
        return destDir;
    }

    @Override
    protected File getSourceDestDir()
    {
        return sourceDestDir;
    }

    @Override
    protected File getDefaultSrcOut()
    {
        return new File( project.getBuild().getDirectory(), "generated-sources/wsimport" );
    }

    @Override
    protected File getImplDestDir()
    {
        return implDestDir;
    }

    @Override
    protected List<String> getWSDLFileLookupClasspathElements()
    {
        List<String> list = new ArrayList<String>();

        for ( Artifact a : project.getDependencyArtifacts() )
        {
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() )
                    || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
            {
                File file = a.getFile();
                if ( file != null )
                {
                    list.add( file.getPath() );
                }
            }
        }

        return list;
    }
}
