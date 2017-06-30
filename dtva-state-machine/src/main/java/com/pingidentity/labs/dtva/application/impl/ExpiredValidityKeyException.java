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
import java.util.Optional;

import javax.validation.constraints.NotNull;

public class ExpiredValidityKeyException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final          String  sessionReference;
	private final @NotNull Instant expiredAt;
	private final @NotNull Instant processedAt;
	
	public ExpiredValidityKeyException(@NotNull String sessionReference, @NotNull Instant expiredAt, @NotNull Instant processedAt) {
		super();
		this.sessionReference = sessionReference;
		this.expiredAt = expiredAt;
		this.processedAt = processedAt;
	}

	public ExpiredValidityKeyException(@NotNull Instant expiredAt, @NotNull Instant processedAt) {
		super();
		this.sessionReference = null;
		this.expiredAt = expiredAt;
		this.processedAt = processedAt;
	}

	@Override
	public String getMessage() {
		return Optional.ofNullable(sessionReference).map((ref)-> "Session \"" + ref + "\"").orElse("Unspecified session")
				+ "\" expired at " + expiredAt.toString() + ". This was detected while processing at " + processedAt.toString();
	}
	
	public Optional<String> getSessionReference() {
		return Optional.ofNullable(sessionReference);
	}

	public Instant getExpiredAt() {
		return expiredAt;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}
}
