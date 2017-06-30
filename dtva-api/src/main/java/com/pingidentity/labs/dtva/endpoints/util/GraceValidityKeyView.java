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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;

public class GraceValidityKeyView implements ValidityKeyView {

	private final Instant evaluatedAt;
	private final SessionIdentifier sid;
	private final Issuer issuer;
	private final Instant consensusGrace;
	
	public GraceValidityKeyView(Instant evaluatedAt, SessionIdentifier sid, Issuer issuer) {
		Objects.requireNonNull(evaluatedAt);
		Objects.requireNonNull(sid);
		Objects.requireNonNull(issuer);
		
		this.evaluatedAt = evaluatedAt;
		this.sid = sid;
		this.issuer = issuer;
		this.consensusGrace = sid.getConsensusGrace().orElseThrow(
				() -> new IllegalStateException("consensus grace time is required for a grace validity view"));
		if (evaluatedAt.isAfter(consensusGrace)) {
			throw new IllegalStateException("evaluation is after consensus grace expires");
		}
	}
	
	@Override
	public ValidityKey getValidityKey() {
		return sid.getKey();
	}

	@Override
	public Issuer getIssuer() {
		return issuer;
	}

	@Override
	public String getStateName() {
		return "grace";
	}

	@Override
	public Instant getEvaluatedAt() {
		return evaluatedAt;
	}

	@Override
	public Instant getLastModifiedAt() {
		return evaluatedAt;
	}

	@Override
	public Instant getScheduledTransitionAt() {
		return consensusGrace;
	}

	@Override
	public Duration untilNextTransition() {
		return Duration.between(evaluatedAt, consensusGrace);
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public boolean isExpired() {
		return false;
	}

	@Override
	public boolean isInvalidated() {
		return false;
	}

	@Override
	public Optional<Instant> getInvalidatedAt() {
		return Optional.empty();
	}
}
