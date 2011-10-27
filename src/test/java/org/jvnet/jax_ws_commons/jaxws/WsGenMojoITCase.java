/*
 * Copyright 2011 Lukas Jungmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jvnet.jax_ws_commons.jaxws;

import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author Lukas Jungmann
 */
public class WsGenMojoITCase {

    private static final File PROJECTS_DIR = new File(System.getProperty("it.projects.dir"));
    private File project;

    public WsGenMojoITCase() {
    }

    @Test
    public void wsgen217() throws IOException {
        project = new File(PROJECTS_DIR, "wsgen217");

        //check EchoService
        assertFilePresent("target/custom/sources/org/codehaus/mojo/jaxws/test/jaxws/EchoResponse.java");
        assertFilePresent("target/custom/classes/org/codehaus/mojo/jaxws/test/jaxws/Echo.class");
        assertFilePresent("target/classes/org/codehaus/mojo/jaxws/test/EchoService.class");
        assertFilePresent("target/wsdl/EchoService.wsdl");
        assertFilePresent("target/wsdl/EchoService_schema1.xsd");
        assertFileNotPresent("target/jaxws/wsgen/wsdl/EchoService.wsdl");

        //check AddService
        assertFilePresent("target/classes/org/codehaus/mojo/jaxws/test/jaxws/Add.class");
        assertFilePresent("target/classes/org/codehaus/mojo/jaxws/test/AddService.class");
        assertFileNotPresent("target/classes/org/codehaus/mojo/jaxws/test/jaxws/Add.java");
        assertFileNotPresent("target/classes/org/codehaus/mojo/jaxws/test/AddService.java");
        assertFileNotPresent("target/wsdl/AddService.wsdl");
        assertFileNotPresent("target/jaxws/wsgen/wsdl/AddService.wsdl");

        //check TService
        assertFilePresent("target/jaxws/wsgen/wsdl/TService.wsdl");
        assertFilePresent("target/jaxws/wsgen/wsdl/ExService.wsdl");
        assertFilePresent("target/test-classes/org/codehaus/mojo/jaxws/test/TService.class");
        assertFilePresent("target/test-classes/org/codehaus/mojo/jaxws/test/jaxws/HelloResponse.class");
        assertFileNotPresent("target/test-classes/org/codehaus/mojo/jaxws/test/TService.java");
        assertFileNotPresent("target/test-classes/org/codehaus/mojo/jaxws/test/jaxws/HelloResponse.java");
    }

    private void assertFilePresent(String path) {
        File f = new File(project, path);
        Assert.assertTrue(f.exists(), "Not found " + f.getAbsolutePath());
    }

    private void assertFileNotPresent(String path) {
        File f = new File(project, path);
        Assert.assertFalse(f.exists(), "Found " + f.getAbsolutePath());
    }
}
