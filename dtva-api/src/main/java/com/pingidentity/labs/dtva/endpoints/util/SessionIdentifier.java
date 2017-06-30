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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.ValidityKey;

import com.github.dwaite.cyborg.electrode.impl.CborDataInput;
import com.github.dwaite.cyborg.electrode.impl.CborOutput;

// IDP-provided session identifier. Consists of a validity key (which is a compound key that contains the
// necessary configuration info) and a consensus grace value describing a grace cutoff before which the
// key does not need to be backed by consensus
public class SessionIdentifier {
	@NotNull private final ValidityKey key;
	private final Instant consensusGrace;
	
	public SessionIdentifier(@NotNull ValidityKey key, Instant consensusGrace) {
		Objects.requireNonNull(key);
		this.key = key;
		if (consensusGrace != null) {
			this.consensusGrace = consensusGrace.truncatedTo(ChronoUnit.SECONDS);
		}
		else {
			this.consensusGrace = null;
		}
	}
	
	public ValidityKey getKey() {
		return key;
	}
	
	public Optional<Instant> getConsensusGrace() {
		return Optional.ofNullable(consensusGrace);
	}
	
	public void writeExternal(@NotNull DataOutput dout) throws IOException {
		Objects.requireNonNull(dout);
		CborOutput cout = new CborOutput(dout);
		cout.writeStartArray(2);
		key.writeExternal(dout);
		if (consensusGrace != null) {
			cout.writeLong(consensusGrace.getEpochSecond());
		} else {
			cout.writeNull();
		}		
	}
	
	public SessionIdentifier(@NotNull DataInput din) throws IOException {
		Objects.requireNonNull(din);
		CborDataInput cin = new CborDataInput(din);
		if (cin.readStartArray() != 2) {
			throw new IOException("Unexpected 2-element array");
		}
		this.key = new ValidityKey(din);
		Instant consensusGrace = null;
		if (cin.peek().isNull()) {
			cin.readNull();
		} else {
			consensusGrace = Instant.ofEpochSecond(cin.readLong());
		}
		this.consensusGrace = consensusGrace;
	}
	
	public static SessionIdentifier fromStringIdentifier(String identifier) throws IllegalArgumentException {
		byte[] cborEncoded = Base64.getUrlDecoder().decode(identifier);
		try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(cborEncoded))) {
			return new SessionIdentifier(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("Session Identifier Format not legal", e);
		}
 	}
	
	public String toStringIdentifier() {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(baos);) {
			writeExternal(dout);
			return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
		} catch (IOException e) {
			throw new IllegalArgumentException("Unexpected IO exception", e);
		}
	}

	public boolean isInGrace(Instant now) {
		return getConsensusGrace().map( (grace) -> grace.isAfter(now)).orElse(false);
	}
}
