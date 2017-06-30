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

import com.pingidentity.labs.dtva.application.ValidityKey;

import com.github.dwaite.cyborg.electrode.CborException;
import com.github.dwaite.cyborg.electrode.impl.CborDataInput;
import com.github.dwaite.cyborg.electrode.impl.CborOutput;

public interface ValidityKeyTransaction extends DTVATransaction {
	public ValidityKey getKey();
	
	default void writeExternal(DataOutput dos) throws IOException {
		CborOutput output = new CborOutput(dos);
		output.writeStartArray(2);
		output.writeLong(getType().ordinal());
		getKey().writeExternal(dos);
	}
	
	public static ValidityKey parse(DataInput in, Type type) throws IOException {
		CborDataInput input = new CborDataInput(in);

		try {
			if (input.readStartArray() != 2) {
				throw new CborException("Expected transaction as two-element array");
			}
			int txtype = input.readInteger();
			if (txtype != type.ordinal()) {
				throw new CborException("expected transaction type " + type);
			}
			return new ValidityKey(in);
		}
		catch (CborException e) {
			throw new IOException("exception processing transaction", e);
		}
	}

}