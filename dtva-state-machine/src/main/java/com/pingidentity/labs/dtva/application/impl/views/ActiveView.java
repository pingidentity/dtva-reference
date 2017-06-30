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
package com.pingidentity.labs.dtva.application.impl.views;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.ValidityKey;

public class ActiveView extends BaseView {
	private final @NotNull Instant nextTransitionAt;
	private final @NotNull Instant lastActivityAt;

	public ActiveView(
			@NotNull ValidityKey validityKey,
			@NotNull Issuer issuer,
			@NotNull Instant lastActivityAt,
			@NotNull Instant evaluatedAt) {
		super(validityKey, issuer, evaluatedAt);
		this.lastActivityAt = lastActivityAt;
		
		Instant inactivityTimeout = getInteractivityTimeout()
				.map((span) -> lastActivityAt.plus(span)).orElse(getHardExpiryAt());
		this.nextTransitionAt = min(inactivityTimeout, getHardExpiryAt());
	}

	private <T extends Comparable<T>> T min(T v1, T v2) {
		int comparison = v1.compareTo(v2);
		return (comparison <= 0) ? v1 : v2;
	}

	@Override
	public String getStateName() {
		return "active";
	}

	@Override
	public Instant getScheduledTransitionAt() {
		return nextTransitionAt;
	}

	@Override
	public Duration untilNextTransition() {
		return Duration.between(getEvaluatedAt(), nextTransitionAt);
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
	public Instant getLastModifiedAt() {
		return lastActivityAt;
	}

	@Override
	public Optional<Instant> getInvalidatedAt() {
		return Optional.empty();
	}
}
