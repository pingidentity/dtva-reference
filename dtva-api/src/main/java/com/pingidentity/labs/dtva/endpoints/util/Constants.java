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
package com.pingidentity.labs.dtva.endpoints.util;

import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

public class Constants {

	static final String VALIDITY_PATH_PREFIX 	= "/validity/";

	static final String SCHEDULED_TRANSITION_AT = "scheduledTransitionAt";
	static final String LAST_MODIFIED_AT		= "lastModifiedAt";
	
	static final String SEXP = "sexp";
	static final String SID = "sid";
	static final String ISS = "iss";
	static final String ITO = "interactivity_timeout";

	public static final StatusType PERMANENT_REDIRECT = new StatusType() {
		@Override
		public int getStatusCode() {
			return 308;
		}
		
		@Override
		public String getReasonPhrase() {
			return "Permanent Redirect";
		}
		
		@Override
		public Family getFamily() {
			return Family.REDIRECTION;
		}
	};

	public static final String APPLICATION_CBOR = "application/cbor";
	public static final MediaType APPLICATION_CBOR_TYPE = MediaType.valueOf(APPLICATION_CBOR);
}
