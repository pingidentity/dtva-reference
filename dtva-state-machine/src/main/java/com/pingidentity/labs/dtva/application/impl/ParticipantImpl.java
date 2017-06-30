/*
 * Copyright (c) 2017 Ping Identity
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pingidentity.labs.dtva.application.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.pingidentity.labs.dtva.application.Participant;

import com.github.dwaite.bytestring.Bytes;

public class ParticipantImpl implements Participant {
	private final String displayName;
	private final Bytes identifier;
	private final boolean isTokenIssuer;

	public ParticipantImpl(String displayName, Bytes identifier, boolean isTokenIssuer) {
		Objects.requireNonNull(identifier);
		this.displayName = displayName;
		this.identifier = identifier;
		this.isTokenIssuer = isTokenIssuer;
	}
	
	public static class Builder {
		private String displayName;
		private Bytes identifier;
		private boolean isTokenIssuer;
		
		public Builder() {}
		
		public Builder(Participant address) {
			displayName = address.getDisplayName().orElse(null);
			identifier = address.getIdentifier();
			isTokenIssuer = address.isTokenIssuer();
		}
		
		public ParticipantImpl build () {
			return new ParticipantImpl(displayName, identifier, isTokenIssuer);
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public Bytes getIdentifier() {
			return identifier;
		}

		public void setIdentifier(Bytes identifier) {
			this.identifier = identifier;
		}

		public boolean isTokenIssuer() {
			return isTokenIssuer;
		}

		public void setTokenIssuer(boolean isTokenIssuer) {
			this.isTokenIssuer = isTokenIssuer;
		}
	}

	public Optional<String> getDisplayName() {
		return Optional.ofNullable(displayName);
	}

	public Bytes getIdentifier() {
		return identifier;
	}

	public boolean isTokenIssuer() {
		return isTokenIssuer;
	}

	public static List<? extends Participant> fromJsonList(JsonArray jsonArray) {
		return jsonArray.stream().
		filter(JsonObject.class::isInstance).
		map(JsonObject.class::cast).
		map(obj -> new ParticipantImpl(
				Optional.ofNullable(obj.get("nickname")).map(Object::toString).orElse(obj.getString("name")),
				hexStringToByteArray(obj.get("identifier").toString()),
				obj.getBoolean("isTokenIssuer", false)
			)).
		collect(Collectors.toList());
	}
	private static Bytes hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return new Bytes(data);
	}
}


