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
package com.pingidentity.labs.dtva.application;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.dwaite.cyborg.electrode.CborException;
import com.github.dwaite.cyborg.electrode.impl.CborDataInput;
import com.github.dwaite.cyborg.electrode.impl.CborOutput;

/**
 * A key representing the validity of token(s). This is a compound key, made up of the rules necessary
 * for evaluating the individual token validity.
 * 
 * The expectation is that this key is included encoded within a field of the token(s), such as a JWT
 * session identifier (`sid`). Such an encoding could contain additional information, such as how to
 * deal with the token being evaluated by a third party before consensus on the validity key registration
 * has succeeded. 
 */
public final class ValidityKey implements Comparable<ValidityKey>, Cloneable {
	private final @NotNull Instant  hardExpiryAt;
	private final @NotNull long     issuerIndex;
	private final          Duration interactivityTimeout;
	private final @NotNull long     nonce;
	
	/**
	 * Create a new validity key from the consituent parameters
	 * 
	 * @param hardExpiryAt the hard expiry time of the token(s) this validity key refers to.
	 * @param issuerIndex the index of a previously registered issuer name within {@link StateImpl#getIssuers()}
	 * @param interactivityTimeout the interactivity timeout of the token(s), or {@link Optional#empty()} if 
	 * interactivity is not to be tracked
	 * @param nonce a differentiating value to distinguish multiple validity keys to be created with the same
	 * hard expiry time and interactivity timeout by the same issuer. May be random or sequential.
	 */
	public ValidityKey(@NotNull Instant hardExpiryAt,
			long issuerIndex,
			@NotNull Optional<Duration> interactivityTimeout,
			long nonce) {
		this.hardExpiryAt  = hardExpiryAt.truncatedTo(ChronoUnit.SECONDS);
		this.issuerIndex = issuerIndex;
		this.interactivityTimeout = interactivityTimeout.orElse(null);
		this.nonce = nonce;
	}
	
	/**
	 * Create a new validity key by decoding from an input stream.
	 * 
	 * A validity key is variable-length encoded via CBOR. Only the data needed for the key is read
	 * from the supplied {@link DataInput}, allowing this constructor to be used as part of a larger
	 * deserialization process.
	 * 
	 * @param dis DataInput object to read values from
	 * @throws IOException on issue with DataInput or validity errors.
	 */
	public ValidityKey(@NotNull DataInput dis) throws IOException {
		
		CborDataInput input = new CborDataInput(dis);
		
		try {
			int count = input.readStartArray();
			if (count != 4) {
				throw new CborException("Expected validity key to have four elements");
			}
			hardExpiryAt = Instant.ofEpochSecond(input.readLong());

			issuerIndex  = input.readLong();
			
			switch (input.peek().getInitialByte().getLogicalType()) {
			case INTEGRAL:
				interactivityTimeout = Duration.ofSeconds(input.readLong());
				break;
			case NULL:
				input.readNull();
				interactivityTimeout = null;
				break;
			default:
				throw new CborException("Expected unsigned integer or null for interactivity timeout");	
			}
			nonce              = input.readLong();
		} catch (CborException e) {
			throw new IOException("Error parsing validity key", e);
		}
		
	}
	
	public void writeExternal(@NotNull DataOutput out) throws IOException {
		CborOutput output = new CborOutput(out);

		output.writeStartArray(4);
		output.writeLong(hardExpiryAt.getEpochSecond());
		output.writeLong(issuerIndex);
		if (interactivityTimeout != null) {
			output.writeLong(interactivityTimeout.getSeconds());
		} else {
			output.writeNull();
		}
		output.writeLong(nonce);
	}
	
	@Override
	public int compareTo(ValidityKey o) {
		if (this == o) {
			return 0;
		}
		int result = hardExpiryAt.compareTo(o.getHardExpiryAt());
		if (result != 0) {
			return result;
		}
		
		result = Long.compare(issuerIndex, o.getIssuerIndex());
		if (result != 0) {
			return result;
		}
		
		int mySpan = getInteractivityTimeout()
			.map(Duration::getSeconds)
			.map(Long::intValue)
			.orElse(0);
		int theirSpan = o.getInteractivityTimeout()
				.map(Duration::getSeconds)
				.map(Long::intValue)
				.orElse(0);
		if (mySpan != theirSpan) {
			return mySpan - theirSpan;
		}
				
		return Long.compare(nonce, o.getNonce());
	}

	public Instant getHardExpiryAt() {
		return hardExpiryAt;
	}
	public int getIssuerIndex() {
		return (int) issuerIndex;
	}

	public long getNonce() {
		return nonce;
	}
	
	@Override
	public int hashCode() {
		return hardExpiryAt.hashCode() << 3 
				^ Long.hashCode(issuerIndex) << 2 
				^ Long.hashCode(nonce) << 1
				^ getInteractivityTimeout().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ValidityKey) {
			ValidityKey o = (ValidityKey) obj;
			return hardExpiryAt.equals(o.hardExpiryAt) &&
					issuerIndex == o.getIssuerIndex() &&
					nonce == o.nonce &&
					getInteractivityTimeout().equals(o.getInteractivityTimeout());
		}
		return false;
	}
	@Override
	protected ValidityKey clone() throws CloneNotSupportedException {
		return (ValidityKey) super.clone();
	}
	
	@Override
	public String toString() {
		return "ValidityKey(maximumExpiry: " + getHardExpiryAt() + 
				", issuerindex: " + getIssuerIndex() +
				", interactivityTimeout: " + getInteractivityTimeout() + 
				", nonce: " + getNonce() + ")";
	}
	
	public static ValidityKey smallestAtInstant(Instant timeCreated) {
		return new ValidityKey(timeCreated, 0L, Optional.of(Duration.ZERO), 0);
	}	
	public Optional<Duration> getInteractivityTimeout() {
		return Optional.ofNullable(interactivityTimeout);
	}
}