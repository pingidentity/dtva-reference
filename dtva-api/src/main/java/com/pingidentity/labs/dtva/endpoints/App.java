/* Copyright 2017 Ping Identity Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */
package com.pingidentity.labs.dtva.endpoints;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import com.pingidentity.labs.dtva.application.DTVACoordinator;
import com.pingidentity.labs.dtva.endpoints.util.IssuersBodyWriter;
import com.pingidentity.labs.dtva.endpoints.util.SessionIdentifierParamConverterProvider;
import com.pingidentity.labs.dtva.endpoints.util.SessionIdentifierViewWriter;
import com.pingidentity.labs.dtva.endpoints.util.ValidityKeyCreationRequestBodyReader;

public class App extends Application {
	private final DTVACoordinator platformInstance;

	public App(@Context ServletContext context) {
		platformInstance = (DTVACoordinator) context.getAttribute("com.pingidentity.labs.dtva.state.PlatformInstance");
		if (platformInstance == null) {
			throw new IllegalStateException("no platform in ServletContext");
		}
	}

	@Override
	public Set<Class<?>> getClasses() {
		return new HashSet<>(Arrays.asList(
				IssuersBodyWriter.class,
				SessionIdentifierParamConverterProvider.class,
				SessionIdentifierViewWriter.class,
				ValidityKeyCreationRequestBodyReader.class
				));
	}
	
	@Override
	public Set<Object> getSingletons() {
		return new HashSet<>(Arrays.asList(
				new SessionIdentifierCollectionEndpoint(platformInstance),
				new IssuerNameCollectionEndpoint(platformInstance)
				));
	}
}