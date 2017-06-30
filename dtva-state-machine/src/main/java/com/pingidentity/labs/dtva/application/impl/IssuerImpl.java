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

import javax.validation.constraints.NotNull;

public class IssuerImpl {

	private final @NotNull String issuerName;
	private final @NotNull int issuingParticipant;
	private final int index;

	public IssuerImpl(@NotNull String name, @NotNull int issuingParticipant, int index) {
		this.issuerName = name;
		this.issuingParticipant = issuingParticipant;
		this.index = index;
	}

	public String getIssuerName() {
		return issuerName;
	}

	public int getIssuingParticipant() {
		return issuingParticipant;
	}

	public int getIndex() {
		return index;
	}
}
