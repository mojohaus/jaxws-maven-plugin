/*
 * Copyright 2011-2012 Oracle.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
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
        assertFilePresent("target/custom/sources/org/jvnet/jax_ws_commons/jaxws/test/jaxws/EchoResponse.java");
        assertFilePresent("target/custom/classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/Echo.class");
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/EchoService.class");
        //-wsdl[...]
        assertFilePresent("target/wsdl/EchoService.wsdl");
        assertFilePresent("target/wsdl/EchoService_schema1.xsd");
        assertFileNotPresent("target/jaxws/wsgen/wsdl/EchoService.wsdl");
        assertFileNotPresent("target/generated-sources/wsdl/EchoService.wsdl");
        assertFileNotPresent("target/generated-sources/test-wsdl/EchoService.wsdl");
        //-wsdl:Xsoap12 + -extension
        assertFileContains("target/wsdl/EchoService.wsdl", "http://schemas.xmlsoap.org/wsdl/soap12/");
        //dependency on 2.1.7
        assertFileContains("target/wsdl/EchoService.wsdl", "JAX-WS RI 2.1.7");

        //check AddService
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/Add.class");
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/AddService.class");
        assertFileNotPresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/Add.java");
        assertFileNotPresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/AddService.java");
        assertFileNotPresent("target/wsdl/AddService.wsdl");
        assertFileNotPresent("target/jaxws/wsgen/wsdl/AddService.wsdl");

        //check TService
        assertFilePresent("target/generated-sources/test-wsdl/TService.wsdl");
        assertFilePresent("target/generated-sources/test-wsdl/ExService.wsdl");
        assertFilePresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/TService.class");
        assertFilePresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/HelloResponse.class");
        assertFilePresent("target/generated-sources/test-wsgen/org/jvnet/jax_ws_commons/jaxws/test/jaxws/HelloResponse.java");
        assertFileNotPresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/HelloResponse.java");
        //dependency on 2.1.7
        assertFileContains("target/generated-sources/test-wsdl/ExService.wsdl", "JAX-WS RI 2.1.7");
        //-portname
        assertFileContains("target/generated-sources/test-wsdl/ExService.wsdl", "port name=\"ExPort\"");
        //-servicename
        assertFileContains("target/generated-sources/test-wsdl/ExService.wsdl", "service name=\"ExService\"");

        //-encoding is not supported, warning should be present
        assertFileContains("build.log", "'-encoding' is not supported by jaxws-tools:2.1.7");

        //package wsdl
        assertJarContains("mojo.it.wsgentest217-2.2.6.jar", "META-INF/wsdl/EchoService.wsdl");
        assertJarContains("mojo.it.wsgentest217-2.2.6.jar", "META-INF/wsdl/EchoService_schema1.xsd");
        assertJarNotContains("mojo.it.wsgentest217-2.2.6.jar", "META-INF/EchoService_schema.xsd");
        assertJarNotContains("mojo.it.wsgentest217-2.2.6.jar", "EchoService_schema.xsd");
        assertJarNotContains("mojo.it.wsgentest217-2.2.6.jar", "META-INF/wsdl/ExService.wsdl");
        assertJarNotContains("mojo.it.wsgentest217-2.2.6.jar", "ExService.wsdl");
    }

    @Test
    public void wsgen22() throws IOException {
        project = new File(PROJECTS_DIR, "wsgen22");
        String v = System.getProperty("jaxws-ri.version");
        //remove 'promoted-' from the version string if needed
        int i = v.indexOf('-');
        int j = v.lastIndexOf('-');
        String version = i != j ? v.substring(0, i) + v.substring(j) : v;

        //check EchoService
        assertFilePresent("target/custom/sources/org/jvnet/jax_ws_commons/jaxws/test/jaxws/EchoResponse.java");
        assertFilePresent("target/custom/classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/Echo.class");
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/EchoService.class");
        //-wsdl[...]
        assertFilePresent("target/wsdl/EchoService.wsdl");
        //-inlineSchemas
        assertFileContains("target/wsdl/EchoService.wsdl", "xs:complexType");
        assertFileNotPresent("target/wsdl/EchoService_schema1.xsd");
        assertFileNotPresent("target/jaxws/wsgen/wsdl/EchoService.wsdl");
        assertFileNotPresent("target/generated-sources/wsdl/EchoService.wsdl");
        assertFileNotPresent("target/generated-sources/test-wsdl/EchoService.wsdl");
        //-wsdl:Xsoap12 + -extension
        assertFileContains("target/wsdl/EchoService.wsdl", "http://schemas.xmlsoap.org/wsdl/soap12/");
        //default dependency on 2.2.x
        assertFileContains("target/wsdl/EchoService.wsdl", "JAX-WS RI " + version);

        //check AddService
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/Add.class");
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/AddService.class");
        assertFileNotPresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/Add.java");
        assertFileNotPresent("target/classes/org/jvnet/jax_ws_commons/jaxws/test/AddService.java");
        assertFileNotPresent("target/wsdl/AddService.wsdl");
        assertFileNotPresent("target/jaxws/wsgen/wsdl/AddService.wsdl");

        //check TService
        assertFilePresent("target/generated-sources/test-wsdl/TService.wsdl");
        assertFilePresent("target/generated-sources/test-wsdl/ExService.wsdl");
        assertFilePresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/TService.class");
        assertFilePresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/HelloResponse.class");
        assertFilePresent("target/generated-sources/test-wsgen/org/jvnet/jax_ws_commons/jaxws/test/jaxws/HelloResponse.java");
        assertFileNotPresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/TService.java");
        assertFileNotPresent("target/test-classes/org/jvnet/jax_ws_commons/jaxws/test/jaxws/HelloResponse.java");
        //default dependency on 2.2.x
        assertFileContains("target/generated-sources/test-wsdl/ExService.wsdl", "JAX-WS RI " + version);
        //-portname
        assertFileContains("target/generated-sources/test-wsdl/ExService.wsdl", "port name=\"ExPort\"");
        //-servicename
        assertFileContains("target/generated-sources/test-wsdl/ExService.wsdl", "service name=\"ExService\"");

        //package wsdl
        assertJarContains("mojo.it.wsgentest22-2.2.6.jar", "META-INF/wsdl/EchoService.wsdl");
        assertJarNotContains("mojo.it.wsgentest22-2.2.6.jar", "META-INF/wsdl/EchoService_schema1.xsd");
        assertJarNotContains("mojo.it.wsgentest22-2.2.6.jar", "META-INF/EchoService_schema.xsd");
        assertJarNotContains("mojo.it.wsgentest22-2.2.6.jar", "EchoService_schema.xsd");
        assertJarNotContains("mojo.it.wsgentest22-2.2.6.jar", "META-INF/wsdl/ExService.wsdl");
        assertJarNotContains("mojo.it.wsgentest22-2.2.6.jar", "ExService.wsdl");
    }

    @Test
    public void jaxwscommons43() throws IOException {
        project = new File(PROJECTS_DIR, "jaxwscommons-43");

        assertFilePresent("target/classes/tests/jaxwscommons43/jaxws/Bye.class");
        assertFilePresent("target/generated-sources/wsdl/WsImplAService.wsdl");
        assertFilePresent("target/generated-sources/wsdl/WsImplBService.wsdl");
        assertFileContains("build.log", "No @javax.jws.WebService found.");
        assertFileContains("build.log", "Skipping tests, nothing to do.");
    }

    @Test
    public void jaxwscommons3() throws IOException {
        project = new File(PROJECTS_DIR, "jaxwscommons-3");

        assertFilePresent("target/cst/WEB-INF/wsdl/NewWebService.wsdl");
        assertJarContains("jaxwscommons-3-1.0.war", "WEB-INF/wsdl/NewWebService.wsdl");
    }

    private void assertFilePresent(String path) {
        File f = new File(project, path);
        Assert.assertTrue(f.exists(), "Not found " + f.getAbsolutePath());
    }

    private void assertFileNotPresent(String path) {
        File f = new File(project, path);
        Assert.assertFalse(f.exists(), "Found " + f.getAbsolutePath());
    }

    private void assertFileContains(String path, String s) throws IOException {
        File f = new File(project, path);
        BufferedReader r = new BufferedReader(new FileReader(f));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.contains(s)) {
                return;
            }
        }
        Assert.fail("'" + s + "' is missing in:" + f.getAbsolutePath());
    }

    private void assertJarContains(String jarName, String path) throws ZipException, IOException {
        File f = new File(project, "target/" + jarName);
        ZipFile zf = new ZipFile(f);
        Assert.assertNotNull(zf.getEntry(path), "'" + path + "' is missing in: " + jarName);
    }

    private void assertJarNotContains(String jarName, String path) throws ZipException, IOException {
        File f = new File(project, "target/" + jarName);
        ZipFile zf = new ZipFile(f);
        Assert.assertNull(zf.getEntry(path), "'" + path + "' is in: " + jarName);
    }
}
