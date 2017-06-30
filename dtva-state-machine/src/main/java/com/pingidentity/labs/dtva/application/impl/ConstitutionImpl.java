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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.Constitution;

import com.github.dwaite.bytestring.Bytes;

public class ConstitutionImpl implements Constitution {
	// permissions of all members
	private final @NotNull List<? extends ParticipantImpl> participants;

	// Maximum time in the future from consensus time that a period can specify
	// it is valid and be accepted by the swirld
	private final @NotNull Duration maxSessionDuration;

	public ConstitutionImpl(@NotNull List<? extends ParticipantImpl> addressBook, @NotNull Duration maxSessionDuration) {
		this.participants = Collections.unmodifiableList(new ArrayList<>(addressBook));
		this.maxSessionDuration = maxSessionDuration;
	}

	/* (non-Javadoc)
	 * @see com.pingidentity.labs.dsm.state.config.Constitutionable#getAddresses()
	 */
	@Override
	public List<? extends ParticipantImpl> getParticipants() {
		return participants;
	}

	public static class Builder {
		private List<ParticipantImpl> addresses;
		private Duration maxSessionDuration;

		public Builder() {
			addresses = new ArrayList<>();
			maxSessionDuration = Duration.ofDays(1);
		}

		public Builder(ConstitutionImpl constitutionImpl) {
			addresses = new ArrayList<ParticipantImpl>(constitutionImpl.getParticipants());
			maxSessionDuration = constitutionImpl.getMaxHardExpiryIn();
		}

		public ConstitutionImpl build() {
			return new ConstitutionImpl(addresses, maxSessionDuration);
		}

		public List<? extends ParticipantImpl> getAddresses() {
			return addresses;
		}

		public void setAddresses(List<ParticipantImpl> addresses) {
			this.addresses = addresses;
		}

		public Duration getMaxSessionDuration() {
			return maxSessionDuration;
		}

		public void setMaxSessionDuration(Duration maxSessionDuration) {
			this.maxSessionDuration = maxSessionDuration;
		}

		public Builder addresses(List<ParticipantImpl> addresses) {
			this.addresses = addresses;
			return this;
		}

		public Builder participantImpl(Consumer<ParticipantImpl.Builder> fn) {
			ParticipantImpl.Builder builder = new ParticipantImpl.Builder();
			fn.accept(builder);
			this.addresses.add(builder.build());
			return this;
		}

		public Builder maxSessionDuration(Duration maxSessionDuration) {
			this.maxSessionDuration = maxSessionDuration;
			return this;
		}
	}
	
	public static Constitution fromJSON(
			List<? extends com.pingidentity.labs.rapport.Peer> addresses,
			JsonObject constitutionValue) {
		Objects.requireNonNull(addresses);
		Objects.requireNonNull(constitutionValue);
		JsonObject constitution = constitutionValue;
		Duration maxSessionDuration = Optional.
				ofNullable(constitution.get("maxSessionDuration")).
				map(JsonNumber.class::cast).
				map(JsonNumber::intValue).
				map(Duration::ofSeconds).
				orElse(Duration.ofHours(24));
		boolean tokenIssuer = constitution.getBoolean("tokenIssuer", false);
		List<? extends ParticipantImpl> constitutionAddresses = 
				addresses.stream().map((a) -> new ParticipantImpl(
						a.getNickname(),
						new Bytes(a.getIdentifier()),
						//FIXME I think this is wrong/global
						tokenIssuer))
		.collect(Collectors.toList());
		
		return new ConstitutionImpl(
				constitutionAddresses,
				maxSessionDuration);
		}

	public Duration getMaxHardExpiryIn() {
		return maxSessionDuration;
	}
}