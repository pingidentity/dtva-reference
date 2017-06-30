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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;
import com.pingidentity.labs.dtva.application.impl.views.ActiveView;
import com.pingidentity.labs.dtva.application.impl.views.ExpiredView;
import com.pingidentity.labs.dtva.application.impl.views.InvalidatedView;

public class ValidityKeyRecord {
//	public Instant getLastModifiedAt();

	private final          Instant  invalidatedAt;
	private final @NotNull Instant  lastActivityAt;
	private final @NotNull Instant  dynamicExpiryAt;
	private final          Duration inactivitySpan;
	private final @NotNull Instant  destructionAt;

	public ValidityKeyRecord(
			@NotNull Instant destructionAt,
			@NotNull Instant createdAt, 
			@NotNull Optional<Duration> inactivitySpan) {
		createdAt = createdAt.truncatedTo(ChronoUnit.SECONDS);

		this.inactivitySpan  = inactivitySpan.orElse(null);
		this.destructionAt   = destructionAt;
		this.lastActivityAt  = createdAt;
		this.invalidatedAt   = null;
		this.dynamicExpiryAt = calculateDynamicExpiry(inactivitySpan, getLastActivityAt(), destructionAt);
	}

	public Optional<ValidityKeyView> toView(Instant now, Issuer issuer, ValidityKey key) {
		if (isDestroyed(now)) {
			return Optional.empty();
		}
		if (isExpired(now)) {
			return Optional.<ValidityKeyView>of(new ExpiredView(key, issuer, dynamicExpiryAt, now));
		}
		if (isInvalidated(now)) {
			return Optional.<ValidityKeyView>of(new InvalidatedView(key, issuer, invalidatedAt, now));
		}
		return Optional.<ValidityKeyView>of(new ActiveView(key, issuer, lastActivityAt, now));
	}

	private boolean isInvalidated(Instant now) {
		return invalidatedAt != null && now.isAfter(invalidatedAt);
	}

	private boolean isDestroyed(Instant now) {
		return now.isAfter(destructionAt);
	}

	private boolean isExpired(Instant now) {
		return now.isAfter(dynamicExpiryAt);
	}
	private static Instant calculateDynamicExpiry(Optional<Duration> inactivityTimespan, Instant lastActivity, Instant mandatoryExpiryAt ) {
		if (!inactivityTimespan.isPresent()) {
			return mandatoryExpiryAt;
		}
		return inactivityTimespan.map((duration) -> {
			if (duration.isNegative() || duration.isZero())
				return null;
			
			Instant inactivityExpiryAt = lastActivity.plus(duration);
			return inactivityExpiryAt.isBefore(mandatoryExpiryAt) ?
					inactivityExpiryAt: mandatoryExpiryAt;			
		}).orElseThrow(IllegalArgumentException::new);
		}

	public Optional<ValidityKeyRecord> updated(Instant now){
		now = now.truncatedTo(ChronoUnit.SECONDS);
		if (isDestroyed(now) || isExpired(now) || getInvalidatedAt().isPresent()) {
			return Optional.empty();
		}
		return Optional.of(new ValidityKeyRecord(this, now));
	}
	
	public Optional<ValidityKeyRecord> invalidated(Instant now) {
		now = now.truncatedTo(ChronoUnit.SECONDS);
		if (isDestroyed(now) || isExpired(now) || getInvalidatedAt().isPresent()) {
			return Optional.empty();
		}
		return Optional.of(new ValidityKeyRecord(this, now, true));
	}
	private ValidityKeyRecord(ValidityKeyRecord session, Instant newerActivity) {
		destructionAt   = session.destructionAt;
		inactivitySpan  = session.inactivitySpan;
		invalidatedAt   = session.getInvalidatedAt().orElse(null);

		lastActivityAt  = newerActivity;
		dynamicExpiryAt = calculateDynamicExpiry(Optional.ofNullable(inactivitySpan), getLastActivityAt(), destructionAt);
	}
	
	private ValidityKeyRecord(ValidityKeyRecord session, Instant now, boolean invalidate) {
		if (!invalidate) {
			throw new IllegalStateException();
		}

		destructionAt   = session.destructionAt;
		inactivitySpan  = session.inactivitySpan;
		lastActivityAt  = session.getLastActivityAt();
		dynamicExpiryAt = session.dynamicExpiryAt;
		invalidatedAt   = now;
	}

	public Optional<Instant> getInvalidatedAt() {
		return Optional.ofNullable(invalidatedAt);
	}

	public Instant getLastActivityAt() {
		return lastActivityAt;
	}
	
	public Instant getLastModifiedAt() {
		// FIXME
		return lastActivityAt;
	}
}