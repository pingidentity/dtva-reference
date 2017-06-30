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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a snapshot of the state of the system, further evaluating that state after a supplied 
 * point in time.
 */
public interface State {

	/** Return the validity keys which have been invalidated by the system */
	Stream<? extends ValidityKeyView> viewInvalidatedValidityKeys();
	
	/** Return all validity keys within the system */
	Stream<? extends ValidityKeyView> viewValidityKeys();

	/** For a given validity key, evaluate it against the state of the system and the view's supplied point
	 * in time.
	 */
	Optional<ValidityKeyView> viewValidityKey(ValidityKey key);

	/** Get the system constitution */
	Constitution getConstitution();
	
	/** Get a list of all known issuerImpls */
	List<Issuer> getIssuers();
	
	public Optional<Issuer> getIssuerByName(String issuerName);
}
