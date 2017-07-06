/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 MojoHaus
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

// Verify that wsdl files have been generated
def wsdlA = new File( basedir, 'target/generated-sources/wsdl/WsImplAService.wsdl' )
assert wsdlA.exists()
def wsdlB = new File( basedir, 'target/generated-sources/wsdl/WsImplBService.wsdl' )
assert wsdlB.exists()
def wsdlC = new File( basedir, 'target/generated-sources/wsdl/WsImplCService.wsdl' )
assert !wsdlC.exists() // shouldn't exist, no WebService annotation

// Verify that portable artifacts have been generated
def byeFile = new File( basedir, 'target/generated-sources/wsgen/tests/jaxwscommons43/jaxws/Bye.java' )
assert byeFile.exists()
def byeClass = new File( basedir, 'target/classes/tests/jaxwscommons43/jaxws/Bye.class' )
assert byeClass.exists()
def hiFile = new File( basedir, 'target/generated-sources/wsgen/tests/jaxwscommons43/jaxws/Hi.java' )
assert hiFile.exists()
def hiClass = new File( basedir, 'target/classes/tests/jaxwscommons43/jaxws/Hi.class' )
assert hiClass.exists()
def dummyFile = new File( basedir, 'target/generated-sources/wsgen/tests/jaxwscommons43/jaxws/Dummy.java' )
assert !dummyFile.exists() // shouldn't exist, no WebService annotation

// check log for "No @javax.jws.WebService found"
def log = new File( basedir, 'build.log')
log.withReader { reader ->
    def found = false
    while ( line = reader.readLine() ) {
        if ( line.contains('No @javax.jws.WebService found') ) {
            found = true
            break
        }
    }
    if ( !found ) {
        throw new Exception( "Log doesn't contain expected string" )
    }
}
