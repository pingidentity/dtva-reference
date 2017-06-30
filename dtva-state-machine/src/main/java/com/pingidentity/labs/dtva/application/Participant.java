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

import java.util.Optional;

import com.github.dwaite.bytestring.Bytes;

/**
 * Represents a participant in the system.
 */
public interface Participant {
	/** A display name, for use in debugging. If {@link Optional#empty()}, the identifier should be used
	 * (possibly truncated to 6-8 hex characters). */
	public Optional<String> getDisplayName();
	
	/**
	 * An identifier for the participant. This may be a public key fingerprint.
	 * 
	 * @return a copy of the identifier
	 */
	public Bytes getIdentifier();

	/**
	 * Indicate whether this participant possesses the token issuer capability, allowing them to
	 * register new issuer names and validity keys
	 */
	public boolean isTokenIssuer();
}


