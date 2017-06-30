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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

import javax.json.JsonObject;

/** 
 * A domain-specific platform for DSM.
 */
public interface DTVACoordinator {
	/**
	 * Send a transaction to register a new validity key, returning the {@link ValidityKey} object that
	 * is to be sent
	 * @param hardExpiryAt hard expiry time of the token, to be interpreted in concensus time
	 * @param issuerImpl object representing the issuer name. Must be a issuer name registered to the local issuer
	 * @param interactivityTimeout the interactivity timeout for the validity key, or {@link Optional#empty()}
	 * if the validity key is not tracking interactivity.
	 */
	public ValidityKey sendValidityKeyRegistration(Instant hardExpiryAt, Issuer issuer, Optional<Duration> interactivityTimeout);

	/**
	 * Send a transaction to register a new validity key, which has been completely formed by the caller.
	 */
	public void sendValidityKeyRegistration(ValidityKey validityKey);
	
	/**
	 * Send a transaction representing interactivity for the given validity key.
	 */
	public void sendValidityKeyInteractivity(ValidityKey key);
	/**
	 * Send a transaction representing invalidation of the given validity key. The local issuer must have
	 * issued the original validity key.
	 */
	public void sendValidityKeyInvalidation(ValidityKey key);
	/**
	 * Register a new issuer name. The local participant must have issuer capability.
	 */
	public void sendIssuerRegistration(String issuerName);
	
	/**
	 * Evaluate a snapshot of the system state, evaluating the state of any validity keys against the
	 * provided instant in time.
	 * 
	 * @param instant in time to evaluate validity keys against
	 * @param stateConsumer function which receives a view of the state and performs some local business
	 *  logic
	 */
	public <R> R withStateEvaluatedAtTime(Instant instant, Function<? super State, ? extends R> stateConsumer);

	public Participant getSelf();
	
	public JsonObject getLocalConfiguration();
	public Optional<Duration> getConsensusGraceSpan();
}