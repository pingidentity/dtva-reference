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

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.json.JsonNumber;
import javax.json.JsonObject;

import com.pingidentity.labs.dtva.application.DTVACoordinator;
import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.Participant;
import com.pingidentity.labs.dtva.application.State;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.transactions.DTVATransaction;
import com.pingidentity.labs.dtva.application.transactions.IssuerRegistrationTransaction;
import com.pingidentity.labs.dtva.application.transactions.ValidityKeyInteractivityTransaction;
import com.pingidentity.labs.dtva.application.transactions.ValidityKeyInvalidationTransaction;
import com.pingidentity.labs.dtva.application.transactions.ValidityKeyRegistrationTransaction;
import com.pingidentity.labs.rapport.Coordinator;

import com.github.dwaite.bytestring.Bytes;

public class DTVACoordinatorImpl implements DTVACoordinator {
	private static SecureRandom random = new SecureRandom();
	
	private Coordinator<StateImpl, DTVATransaction> platform;

	public DTVACoordinatorImpl(Coordinator<StateImpl, DTVATransaction> platform) {
		this.platform = platform;
	}

	@Override
	public <R> R withStateEvaluatedAtTime(Instant instant, Function<? super State, ? extends R> stateConsumer) {
		return platform.withState(
				(state) -> stateConsumer.apply(new StateViewImpl(state, instant)));
	}

	@Override
	public ValidityKey sendValidityKeyRegistration(Instant hardExpiryAt, Issuer issuer,
			Optional<Duration> interactivityTimeout) {
		long issuerIndex = issuer.getIndex();

		long nonce;
		synchronized(this) {
			nonce = random.nextLong() & Long.MAX_VALUE;
		}
		ValidityKey key = new ValidityKey(hardExpiryAt, issuerIndex, interactivityTimeout, nonce);
		sendValidityKeyRegistration(key);
		return key;
	}

	@Override
	public void sendValidityKeyRegistration(ValidityKey validityKey) {
		ValidityKeyRegistrationTransaction tx = new ValidityKeyRegistrationTransaction(validityKey);
		platform.queueTransaction(tx);
	}

	@Override
	public void sendValidityKeyInteractivity(ValidityKey key) {
		ValidityKeyInteractivityTransaction tx = new ValidityKeyInteractivityTransaction(key);
		platform.queueTransaction(tx);
	}

	@Override
	public void sendValidityKeyInvalidation(ValidityKey key) {
		ValidityKeyInvalidationTransaction tx = new ValidityKeyInvalidationTransaction(key);
		platform.queueTransaction(tx);
	}

	@Override
	public void sendIssuerRegistration(String issuerName) {
		IssuerRegistrationTransaction tx = new IssuerRegistrationTransaction(issuerName);
		platform.queueTransaction(tx);
	}

	@Override
	public Participant getSelf() {
		Bytes identifier = new Bytes(platform.getSelf().getIdentifier());
		List<? extends Participant> participants = platform.getState().getConstitution().getParticipants();
		for (Participant participant : participants) {
			if (participant.getIdentifier().equals(identifier)) {
				return participant;
			}
		}
		throw new IllegalStateException("Unable to find self in list of participants");
	}

	@Override
	public JsonObject getLocalConfiguration() {
		return (JsonObject) platform.getLocalConfiguration();
	}

	@Override
	public Optional<Duration> getConsensusGraceSpan() {
		JsonNumber consensusGraceSpan = getLocalConfiguration().getJsonNumber("consensus-grace-span");
		return Optional.ofNullable(consensusGraceSpan).map(JsonNumber::longValue).map(Duration::ofSeconds);
	}
}
