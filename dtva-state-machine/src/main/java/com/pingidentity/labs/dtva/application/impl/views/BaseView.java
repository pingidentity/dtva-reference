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

import java.time.Instant;

import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;

public abstract class BaseView implements ValidityKeyView {
	private final @NotNull ValidityKey validityKey;
	private final @NotNull Instant evaluatedAt;
	private final @NotNull Issuer issuer;
	
	public BaseView(@NotNull ValidityKey validityKey, @NotNull Issuer issuer, @NotNull Instant evaluatedAt) {
		this.validityKey = validityKey;
		this.issuer = issuer;
		this.evaluatedAt = evaluatedAt;

		if (evaluatedAt.isAfter(getHardExpiryAt())) {
			throw new IllegalStateException("evaluating after destruction");
		}
	}

	@Override
	public final Instant getEvaluatedAt() {
		return evaluatedAt;
	}

	public ValidityKey getValidityKey() {
		return validityKey;
	}

	public Issuer getIssuer() {
		return issuer;
	}
}
