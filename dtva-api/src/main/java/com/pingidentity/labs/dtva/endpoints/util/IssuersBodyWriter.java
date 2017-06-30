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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.pingidentity.labs.dtva.application.Issuer;

import com.github.dwaite.cyborg.electrode.impl.CborOutput;


@Provider
@Produces({MediaType.APPLICATION_JSON, Constants.APPLICATION_CBOR})
public class IssuersBodyWriter implements MessageBodyWriter<List<Issuer>> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (List.class.isAssignableFrom(type)
		        && genericType instanceof ParameterizedType) {
		      ParameterizedType parameterizedType = (ParameterizedType) genericType;
		      Type[] actualTypeArgs = (parameterizedType.getActualTypeArguments());
		      return (actualTypeArgs.length == 1 && 
		    		  Issuer.class.isAssignableFrom((Class<?>)actualTypeArgs[0]) &&
		    		  (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) ||
					mediaType.isCompatible(Constants.APPLICATION_CBOR_TYPE)));
		}
		return false;
	}

	@Override
	public long getSize(List<Issuer> t, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(List<Issuer> issuers, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
			writeJson(issuers, httpHeaders, entityStream);
		} else if (mediaType.isCompatible(Constants.APPLICATION_CBOR_TYPE)) {
			writeCbor(issuers, httpHeaders, entityStream);
		} else {
			throw new WebApplicationException("Unsupported media type");
		}
	}

	private void writeCbor(List<Issuer> issuers, MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException {
		try (DataOutputStream dos = new DataOutputStream(entityStream)) {
			CborOutput cout = new CborOutput(dos);
			cout.writeStartArray(issuers.size());
			for (Issuer issuer : issuers) {
				cout.writeText(issuer.getIssuerName());
			}
		}
	}

	private void writeJson(List<Issuer> issuers, MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Issuer issuer : issuers) {
			builder.add(issuer.getIssuerName());
		}
		try (JsonWriter writer = Json.createWriter(entityStream)) {
			writer.writeArray(builder.build());
		}
	}
}
