/*
 * Copyright 2011 Oracle.
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
package org.codehaus.mojo.jaxws.test;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author Lukas Jungmann
 */
@WebService(serviceName = "AddService")
public class AddService {

    /** This is a sample web service operation */
    @WebMethod(operationName = "addNumbers")
    public int add(@WebParam(name = "x") int x, @WebParam(name="y") int y) {
        return x + y;
    }
}
