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

/** 
 * Represents the current state of the validity key within consensus state, 
 * evaluated against a particular instant of time.
 */
public interface ValidityKeyView {
	ValidityKey getValidityKey();
	Issuer getIssuer();
	String getStateName();
	
	Instant getEvaluatedAt();
	Instant getLastModifiedAt();
	
	default Instant getHardExpiryAt() {
		return getValidityKey().getHardExpiryAt();
	}
	
	default Optional<Duration> getInteractivityTimeout() {
		return getValidityKey().getInteractivityTimeout();
	}
	
	default Duration untilHardExpiry() {
		Duration untilHardExpiry = Duration.between(getEvaluatedAt(), getHardExpiryAt());
		if (untilHardExpiry.isNegative()) {
			return Duration.ZERO;
		}
		return untilHardExpiry;
	}

	Instant getScheduledTransitionAt();
	Duration untilNextTransition();
	
	boolean isActive();
	boolean isExpired();
	boolean isInvalidated();
	
	Optional<Instant> getInvalidatedAt();
}
