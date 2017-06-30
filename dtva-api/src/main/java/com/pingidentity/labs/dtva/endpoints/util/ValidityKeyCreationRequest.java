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
import java.util.Optional;

import com.pingidentity.labs.dtva.application.ValidityKey;

public class ValidityKeyCreationRequest {

	private final Instant sexp;
	private final Optional<Duration> interactivityTimeout;
	private final String iss;
	
	public ValidityKeyCreationRequest(Instant sexp, Optional<Duration> interactivityTimeout, String iss) {
		this.sexp = sexp;
		this.interactivityTimeout = interactivityTimeout;
		this.iss = iss;
	}
	public boolean matches(ValidityKey validityKey) {
		return validityKey.getHardExpiryAt().equals(getHardExpiryAt()) &&
		validityKey.getInteractivityTimeout().equals(getInteractivityTimeout());
	}
	public Instant getHardExpiryAt() {
		return sexp;
	}
	public Optional<Duration> getInteractivityTimeout() {
		return interactivityTimeout;
	}
	public String getIssuerName() {
		return iss;
	}
}