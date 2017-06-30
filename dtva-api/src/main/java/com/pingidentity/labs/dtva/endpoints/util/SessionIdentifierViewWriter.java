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

import static com.pingidentity.labs.dtva.endpoints.util.Constants.ISS;
import static com.pingidentity.labs.dtva.endpoints.util.Constants.ITO;
import static com.pingidentity.labs.dtva.endpoints.util.Constants.SEXP;
import static com.pingidentity.labs.dtva.endpoints.util.Constants.SID;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.github.dwaite.cyborg.electrode.impl.CborOutput;

@Provider
@Produces({MediaType.APPLICATION_JSON, Constants.APPLICATION_CBOR})
public class SessionIdentifierViewWriter implements MessageBodyWriter<SessionIdentifierView> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return SessionIdentifierView.class.isAssignableFrom(type) && 
				(mediaType.equals(MediaType.APPLICATION_JSON_TYPE) || mediaType.equals(Constants.APPLICATION_CBOR_TYPE));
	}

	@Override
	public long getSize(SessionIdentifierView t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(SessionIdentifierView sessionIdentifierView, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
	
		httpHeaders.put("Last-Modified", Collections.singletonList(toHTTPDate(sessionIdentifierView.getLastModifiedAt())));
		httpHeaders.put("Expires", Collections.singletonList(toHTTPDate(sessionIdentifierView.getHardExpiryAt())));

		if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
			try(JsonGenerator generator = Json.createGenerator(entityStream)) {
				generator.writeStartObject();
				generator.write(SEXP, sessionIdentifierView.getHardExpiryAt().getEpochSecond());
				generator.write(SID, sessionIdentifierView.getSid().toStringIdentifier());
				generator.write(ISS, sessionIdentifierView.getIssuerName());
				sessionIdentifierView.getInteractivityTimeout().ifPresent((inactivityTimespan) -> {
					generator.write(ITO, inactivityTimespan.getSeconds());
				});
				generator.writeEnd();
			}
		}
		else if (mediaType.equals(Constants.APPLICATION_CBOR_TYPE)) {
			try(DataOutputStream dos = new DataOutputStream(entityStream)) {
				CborOutput cout = new CborOutput(dos);
				int pairs = sessionIdentifierView.getInteractivityTimeout().isPresent() ? 4 : 3;
				cout.writeStartMap(pairs)
				.writeText(SEXP).writeLong(sessionIdentifierView.getHardExpiryAt().getEpochSecond())
				.writeText(SID).writeText(sessionIdentifierView.getSid().toStringIdentifier())
				.writeText(ISS).writeText(sessionIdentifierView.getIssuerName());
				if (sessionIdentifierView.getInteractivityTimeout().isPresent()) {
					cout.writeText(ITO).writeLong(sessionIdentifierView.getInteractivityTimeout().get().getSeconds());
				}
			}
		}
	}

	public static String toHTTPDate(Instant instant) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("eee, dd MMM yyyy HH:mm:ss zzz").withZone(ZoneId.of("GMT"));
		return formatter.format(instant);
	}
}
