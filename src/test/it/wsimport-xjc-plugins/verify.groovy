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

// Verify that files have been generated with XJC plugins
def helloSource = new File( basedir, 'target/generated-sources/wsimport/org/jvnet/jax_ws_commons/wsimport/test/Hello.java' )
assert helloSource.exists()
def foundEquals = false
def foundToString = false
helloSource.eachLine { line ->
    if ( line.contains('public boolean equals(') ) {
        foundEquals = true
    } else if ( line.contains('public String toString(') ) {
        foundToString = true
    }
}
if ( !foundEquals ) {
    throw new Exception( "No equals method has been generated in Hello.java" )
}
if ( !foundToString ) {
    throw new Exception( "No toString method has been generated in Hello.java" )
}

def helloResponseSource = new File( basedir, 'target/generated-sources/wsimport/org/jvnet/jax_ws_commons/wsimport/test/HelloResponse.java' )
assert helloResponseSource.exists()
foundEquals = false
foundToString = false
helloResponseSource.eachLine { line ->
    if ( line.contains('public boolean equals(') ) {
        foundEquals = true
    } else if ( line.contains('public String toString(') ) {
        foundToString = true
    }
}
if ( !foundEquals ) {
    throw new Exception( "No equals method has been generated in HelloResponse.java" )
}
if ( !foundToString ) {
    throw new Exception( "No toString method has been generated in HelloResponse.java" )
}
