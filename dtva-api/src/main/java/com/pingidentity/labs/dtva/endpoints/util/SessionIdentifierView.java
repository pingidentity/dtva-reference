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

import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;

// IDP-provided session identifier. Consists of a validity key (which is a compound key that contains the
// necessary configuration info) and a consensus grace value describing a grace cutoff before which the
// key does not need to be backed by consensus
public class SessionIdentifierView {
	@NotNull private final SessionIdentifier sid;
	@NotNull private final ValidityKeyView view;
	
	public SessionIdentifierView(@NotNull ValidityKeyView view, SessionIdentifier sid) {
		Objects.requireNonNull(view);
		Objects.requireNonNull(sid);
		this.view = view;
		this.sid = sid;
	}
	
	public ValidityKey getKey() {
		return sid.getKey();
	}
	
	public SessionIdentifier getSid() {
		return sid;
	}
	
	public ValidityKeyView getView() {
		return view;
	}

	public Instant getLastModifiedAt() {
		return view.getLastModifiedAt();
	}

	public Instant getHardExpiryAt() {
		return view.getHardExpiryAt();
	}

	public Optional<Duration> getInteractivityTimeout() {
		return view.getInteractivityTimeout();
	}

	public String getIssuerName() {
		return view.getIssuer().getIssuerName();
	}
}
