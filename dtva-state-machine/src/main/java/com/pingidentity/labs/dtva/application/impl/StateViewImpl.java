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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pingidentity.labs.dtva.application.Constitution;
import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.Participant;
import com.pingidentity.labs.dtva.application.State;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;

public class StateViewImpl implements State {
	
	private final StateImpl state;
	private final Instant instant;

	public StateViewImpl(StateImpl state, Instant instant) {
		this.state = state;
		this.instant = instant;
		
	}
	
	@Override
	public Stream<? extends ValidityKeyView> viewInvalidatedValidityKeys() {
		return state.getInvalidatedValidityKeys(instant);
	}

	@Override
	public Stream<? extends ValidityKeyView> viewValidityKeys() {
		return state.getValidityKeys(instant);
	}

	@Override
	public Optional<ValidityKeyView> viewValidityKey(ValidityKey key) {
		return state.viewKeyValidity(instant, key);
	}

	@Override
	public Constitution getConstitution() {
		return state.getConstitution();
	}

	@Override
	public List<Issuer> getIssuers() {
		List<? extends Participant> participants = state.getConstitution().getParticipants();
		return state.getIssuers().stream()
				.map((iss) -> new IssuerViewImpl(iss, participants.get(iss.getIssuingParticipant())))
				.map((iv) -> Issuer.class.cast(iv))
				.collect(Collectors.toList());
	}

	@Override
	public Optional<Issuer> getIssuerByName(String issuerName) {
		List<? extends Participant> participants = state.getConstitution().getParticipants();
		return state.getIssuerByName(issuerName)
				.map((iss) -> new IssuerViewImpl(iss, participants.get(iss.getIssuingParticipant())))
				.map((iv) -> Issuer.class.cast(iv));
	}

}
