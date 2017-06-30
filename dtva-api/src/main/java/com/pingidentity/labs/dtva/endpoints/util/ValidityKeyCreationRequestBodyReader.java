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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.github.dwaite.cyborg.electrode.CborEvent;
import com.github.dwaite.cyborg.electrode.InitialByte;
import com.github.dwaite.cyborg.electrode.InitialByte.LogicalType;
import com.github.dwaite.cyborg.electrode.InitialByte.Major;
import com.github.dwaite.cyborg.electrode.impl.CborDataInput;

@Provider
@Consumes({MediaType.APPLICATION_JSON, Constants.APPLICATION_CBOR})
public class ValidityKeyCreationRequestBodyReader implements MessageBodyReader<ValidityKeyCreationRequest> {
	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(ValidityKeyCreationRequest.class) && (
				mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) ||
				mediaType.isCompatible(Constants.APPLICATION_CBOR_TYPE));
	}

	@Override
	public ValidityKeyCreationRequest readFrom(Class<ValidityKeyCreationRequest> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
			return readFromJson(entityStream);
		}
		if (mediaType.isCompatible(Constants.APPLICATION_CBOR_TYPE)) {
			return readFromCbor(entityStream);
		}
		throw new WebApplicationException("Incompatible media type");
	}

	private ValidityKeyCreationRequest readFromCbor(InputStream entityStream) throws IOException {
		try (DataInputStream dis = new DataInputStream(entityStream)) {
			CborDataInput cin = new CborDataInput(dis);
			int pairCount = cin.readStartMap();
			Instant hardExpiryAt = null;
			String issuer = null;
			Duration interactivityTimeout = null;
			
			for (int i = 0; i < pairCount; i++) {
				String key = cin.readText();
				CborEvent value = cin.read();
				LogicalType type = value.getInitialByte().getLogicalType();
				switch (key) {
				case Constants.SEXP:
					if (value.getInitialByte().getMajor() != Major.INTEGER) {
						throw new IOException("Expected integer key for \"" + Constants.SEXP + "\"");
					}
					hardExpiryAt = Instant.ofEpochSecond(value.additionalInfoAsLong());
					break;
				case Constants.ISS:
					if (type != LogicalType.TEXT_CHUNK) {
						throw new IOException("Expected fixed text for \"" + Constants.ISS + "\"");
					}
					issuer = value.asTextValue();
					break;					
				case Constants.ITO:
					if (value.getInitialByte().getMajor() != Major.INTEGER && value.getInitialByte() != InitialByte.NULL) {
						throw new IOException("Expected integer key or null for \"" + Constants.ITO + "\"");
					}
					if (value.getInitialByte().getMajor() == Major.INTEGER) {
						interactivityTimeout = Duration.ofSeconds(value.additionalInfoAsLong());
					}
					break;
				default:
					// ignore
				}
			}
			return new ValidityKeyCreationRequest(hardExpiryAt, Optional.ofNullable(interactivityTimeout), issuer);
		}
	}

	private ValidityKeyCreationRequest readFromJson(InputStream entityStream) {
		JsonObject object = Json.createReader(entityStream).readObject();		
		
		Optional<Duration> inactivityTimespan = Optional
				.ofNullable(object.getJsonNumber(Constants.ITO))
				.map(JsonNumber::longValue)
				.map(Duration::ofSeconds);		
		Instant destructionAt = Optional
				.ofNullable(object.getJsonNumber(Constants.SEXP))
				.map(JsonNumber::longValue)
				.map(Instant::ofEpochSecond)
				.orElseThrow(()-> new IllegalArgumentException(Constants.SEXP));
		String issuer = Optional.ofNullable(object.getJsonString(Constants.ISS))
				.map(JsonString::getString)
				.orElseThrow(() -> new IllegalArgumentException(Constants.ISS));
		return new ValidityKeyCreationRequest(destructionAt, inactivityTimespan, issuer);
	}
}
