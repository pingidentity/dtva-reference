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
package com.pingidentity.labs.dtva.application.transactions;

import java.io.DataInput;
import java.io.IOException;

import javax.validation.constraints.NotNull;

import com.pingidentity.labs.dtva.application.ValidityKey;

public class ValidityKeyInteractivityTransaction implements ValidityKeyTransaction {
	private @NotNull ValidityKey key;

	public ValidityKeyInteractivityTransaction(@NotNull ValidityKey key) {
		this.key = key;
	}

	public ValidityKeyInteractivityTransaction(DataInput in) throws IOException {
		key = ValidityKeyTransaction.parse(in, getType());
	}

	@Override
	public Type getType() {
		return Type.UpdateInteractivity;
	}

	public ValidityKey getKey() {
		return key;
	}
}
