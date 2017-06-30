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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

import com.pingidentity.labs.dtva.application.Constitution;
import com.pingidentity.labs.dtva.application.Participant;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;

public final class StateImpl {
	public final NavigableMap<ValidityKey, ValidityKeyRecord> periods;
	private final Constitution constitution;
	public final List<IssuerImpl> issuerImpls;
	public final Map<String, IssuerImpl> issuersByName;

	public StateImpl(Constitution constitution) {
		this.constitution = constitution;
		periods = Collections.unmodifiableNavigableMap(new ConcurrentSkipListMap<>());
		issuerImpls = Collections.unmodifiableList(Collections.emptyList());
		issuersByName = Collections.unmodifiableMap(Collections.emptyMap());
	}

	StateImpl(Constitution constitution,
			ConcurrentSkipListMap<ValidityKey, ValidityKeyRecord> periods, List<IssuerImpl> issuerImpls) {
		this.constitution = constitution;
		this.periods = Collections.unmodifiableNavigableMap(periods);
		this.issuerImpls = Collections.unmodifiableList(new ArrayList<>(issuerImpls));
		Map<String, IssuerImpl> issuersByName = new HashMap<String, IssuerImpl>();
		for (IssuerImpl issuerImpl : issuerImpls) {
			issuersByName.put(issuerImpl.getIssuerName(), issuerImpl);
		}
		this.issuersByName = Collections.unmodifiableMap(issuersByName);
	}

	/* (non-Javadoc)
	 * @see com.pingidentity.labs.dsm.state.DSMState#getInvalidated(java.time.Instant)
	 */
	public Stream<? extends ValidityKeyView> getInvalidatedValidityKeys(Instant now) {
		return periods
				.entrySet()
				.stream()
				.map(kv -> toView(kv, now))
				.filter(Objects::nonNull)
				.filter(ip -> ip.isInvalidated());
	}

	// returns null on exception
	private ValidityKeyView toView(Map.Entry<ValidityKey, ? extends ValidityKeyRecord> entry, Instant now) {
		return toView(entry.getKey(), entry.getValue(), now);
	}
	
	private ValidityKeyView toView(ValidityKey key, ValidityKeyRecord record, Instant now) {
		IssuerImpl issuerImpl = issuerImpls.get(key.getIssuerIndex());
		Participant participant = constitution.getParticipants().get(issuerImpl.getIssuingParticipant());
		IssuerViewImpl issuer = new IssuerViewImpl(issuerImpl, participant);
		return record.toView(now, issuer, key).orElse(null);
	}

	/* (non-Javadoc)
	 * @see com.pingidentity.labs.dsm.state.DSMState#getSessions(java.time.Instant)
	 */
	public Stream<? extends ValidityKeyView> getValidityKeys(Instant now) {
		return periods.entrySet().stream()
				.filter(kv -> kv.getKey().getHardExpiryAt().isAfter(now))
				.map(kv -> toView(kv, now))
				.filter(Objects::nonNull);
	}

	/* (non-Javadoc)
	 * @see com.pingidentity.labs.dsm.state.DSMState#getSession(java.time.Instant, com.pingidentity.labs.dsm.state.CompoundKey)
	 */
	public Optional<ValidityKeyView> viewKeyValidity(Instant now, ValidityKey key) {
		Optional<ValidityKeyRecord> record = Optional.ofNullable(periods.get(key));
		return record.map((r) -> toView(key, r, now));
	}

	/* (non-Javadoc)
	 * @see com.pingidentity.labs.dsm.state.DSMState#getConstitution()
	 */
	public Constitution getConstitution() {
		return constitution;
	}

	public List<IssuerImpl> getIssuers() {
		return issuerImpls;
	}
	
	public Optional<IssuerImpl> getIssuerByName(String issuerName) {
		return Optional.ofNullable(issuersByName.get(issuerName));
	}
}
