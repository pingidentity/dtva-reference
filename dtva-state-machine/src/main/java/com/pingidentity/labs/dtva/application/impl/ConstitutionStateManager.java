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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.pingidentity.labs.dtva.application.Participant;
import com.pingidentity.labs.rapport.StateSerializer;

import com.github.dwaite.bytestring.Bytes;
import com.github.dwaite.cyborg.electrode.CborException;
import com.github.dwaite.cyborg.electrode.impl.CborDataInput;
import com.github.dwaite.cyborg.electrode.impl.CborOutput;

public class ConstitutionStateManager implements StateSerializer<ConstitutionImpl> {

	@Override
	public ConstitutionImpl deserializeState(DataInput din) throws IOException {
		try {
			CborDataInput input = new CborDataInput(din);
			if (input.readStartArray() != 2) {
				throw new CborException("expected two element array at root of constitution");
			}
			
			long maxSessionDuration = input.readLong();
			int addressCount = input.readStartArray();
			List<ParticipantImpl> addresses = new ArrayList<>();
			try {
				IntStream.range(0, addressCount).forEach((i) -> {
					try {
						if (input.readStartArray() != 3) {
							throw new CborException("expected three element array for participant");
						}
						String displayName;
						switch (input.peek().getInitialByte().getLogicalType()) {
						case TEXT_CHUNK:
							displayName = input.readText();
							break;
						case NULL:
							displayName = null;
							input.readNull();
							break;
						default:
							throw new CborException("Expected text or null for display name");
						}
						Bytes identifier = input.readBinary();
						boolean isTokenIssuer = input.readBoolean();
						ParticipantImpl participantImpl = new ParticipantImpl(displayName, identifier.toBytes(), isTokenIssuer);
						addresses.add(participantImpl);
					}
					catch (IOException|CborException e) {
						throw new IllegalStateException(e);
					}
				});
				return new ConstitutionImpl(addresses,
						Duration.ofSeconds(maxSessionDuration));
			}
			catch (IllegalStateException e) {
				Throwable inner = e.getCause();
				if (inner instanceof IOException)
					throw (IOException) inner;
				throw e;
			}
		}
		catch (CborException e) {
			throw new IOException("Error processing constitution", e);
		}
	}

	@Override
	public void serializeState(ConstitutionImpl constitutionImpl, DataOutput dout) throws IOException {
		CborOutput output = new CborOutput(dout);
		output.writeStartArray(2)
			.writeLong(constitutionImpl.getMaxHardExpiryIn().getSeconds())
			.writeStartArray(constitutionImpl.getParticipants().size());
		for (Participant participant : constitutionImpl.getParticipants()) {
			output.writeStartArray(3);
			if (participant.getDisplayName().isPresent()) {
				output.writeText(participant.getDisplayName().get());
			}
			else {
				output.writeNull();
			}
			output.writeBytes(participant.getIdentifier());
			output.writeBoolean(participant.isTokenIssuer());
		}
	}
}
