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
import java.io.DataOutput;
import java.io.IOException;

import javax.validation.constraints.NotNull;

import com.github.dwaite.cyborg.electrode.CborException;
import com.github.dwaite.cyborg.electrode.impl.CborDataInput;
import com.github.dwaite.cyborg.electrode.impl.CborOutput;

public class IssuerRegistrationTransaction implements DTVATransaction {

	private @NotNull String issuerName;
	
	public IssuerRegistrationTransaction(@NotNull String issuerName) {
		this.issuerName = issuerName;
	}

	public IssuerRegistrationTransaction(DataInput in) throws IOException, ClassNotFoundException {
		try {
			CborDataInput input = new CborDataInput(in);
			if (input.readStartArray() != 2) {
				throw new CborException("expected two element array");
			}
			if (input.readLong() != getType().ordinal()) {
				throw new CborException("expected register issuer transaction type");
			}
			issuerName = input.readText();
		}
		catch (CborException e) {
			throw new IOException("error processing transaction", e);
		}
	}

	public void writeExternal(DataOutput out) throws IOException {
		new CborOutput(out)
			.writeStartArray(2)
				.writeInteger(getType().ordinal())
				.writeText(issuerName);
	}

	public Type getType() {
		return Type.RegisterIssuer;
	}

	public String getIssuerName() {
		return issuerName;
	}
}