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

// Verify that correct set of files have been generated

// execution id1
def source = new File( basedir, 'target/generated-sources/wsimport/ws1/ProcessOrder.java' )
assert source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws1/ProcessOrder_Service.java' )
assert source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws1/ProcessOrderInDir.java' )
assert !source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws1/CustomOrder.java' )
assert !source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws1/CustomOrder_Service.java' )
assert !source.exists()

// execution id2
source = new File( basedir, 'target/generated-sources/wsimport/ws2/ProcessOrder.java' )
assert source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws2/ProcessOrderInDir.java' )
assert source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws2/ProcessOrder_Service.java' )
assert !source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws2/CustomOrder.java' )
assert !source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws2/CustomOrder_Service.java' )
assert !source.exists()

// execution id3
source = new File( basedir, 'target/generated-sources/wsimport/ws3/CustomOrder.java' )
assert source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws3/CustomOrder_Service.java' )
assert source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws3/ProcessOrder.java' )
assert !source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws3/ProcessOrderInDir.java' )
assert !source.exists()
source = new File( basedir, 'target/generated-sources/wsimport/ws3/ProcessOrder_Service.java' )
assert !source.exists()
