package org.codehaus.mojo.jaxws.it;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * A web service implementation class.
 * @author Dan T. Tran
 *
 */
@WebService(name="HelloWorld", serviceName="HelloWorldService")

public class HelloWorldImpl {

	@WebMethod()
	public String helloWorld( String userName ) {
		return "Hello " + userName;
	}
}
