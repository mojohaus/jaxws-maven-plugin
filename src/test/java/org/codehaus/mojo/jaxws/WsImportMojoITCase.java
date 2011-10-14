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
package org.codehaus.mojo.jaxws;

import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author Lukas Jungmann
 */
public class WsImportMojoITCase {

    private static final File PROJECTS_DIR = new File(System.getProperty("it.projects.dir"));
    private File project;

    public WsImportMojoITCase() {
    }

    @Test
    public void wsimport217() throws IOException {
        project = new File(PROJECTS_DIR, "wsimport217");

        //check HelloWs
        assertFilePresent("target/jaxws/stale/.staleFlag");
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/wsimport/test/HelloWs.class");
        assertFileNotPresent("target/test-classes/org/jvnet/jax_ws_commons/wsimport/test/HelloWs.class");
        assertFilePresent("target/generated-sources/wsimport/org/jvnet/jax_ws_commons/wsimport/test/HelloWs_Service.java");
        //this needs to be fixed as there should be a way to not generate sources
        //assertFileNotPresent("target/jaxws/wsimport/org/jvnet/jax_ws_commons/wsimport/test/HelloWs.java");

        //check sample.wsdl (url)
        assertFilePresent("target/jaxws/stale/.staleFlag2");
        assertFilePresent("target/custom/classes/org/jvnet/jaxwsri/sample/GreetersPortT.class");
        assertFilePresent("target/custom/sources/org/jvnet/jaxwsri/sample/MyGreeter.java");

        //check AddService
        assertFilePresent("target/jaxws/stale/.staleFlag-test");
        assertFilePresent("target/test-classes/wsimport/test/AddService.class");
        assertFilePresent("target/test-classes/wsimport/test/schema/SumType.class");
        assertFilePresent("target/generated-sources/test-wsimport/wsimport/test/SumUtil.java");
        assertFileNotPresent("target/classes/wsimport/test/AddService.class");
    }

    @Test
    public void jaxwscommons49() throws IOException {
        project = new File(PROJECTS_DIR, "jaxwscommons-49");

        assertFilePresent("target/jaxws/stale/.staleFlag");
        assertFilePresent("foo/AddService.wsdl");
        assertFilePresent("src/main/java/org/jvnet/jax_ws_commons/wsimport/test/AddService_Service.java");
        assertFilePresent("target/classes/org/jvnet/jax_ws_commons/wsimport/test/AddService.class");
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
