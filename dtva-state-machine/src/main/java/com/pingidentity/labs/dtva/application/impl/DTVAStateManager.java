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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.IntStream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pingidentity.labs.dtva.application.Constitution;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.transactions.DTVATransaction;
import com.pingidentity.labs.dtva.application.transactions.IssuerRegistrationTransaction;
import com.pingidentity.labs.dtva.application.transactions.ValidityKeyInteractivityTransaction;
import com.pingidentity.labs.dtva.application.transactions.ValidityKeyInvalidationTransaction;
import com.pingidentity.labs.dtva.application.transactions.ValidityKeyRegistrationTransaction;
import com.pingidentity.labs.rapport.Peer;
import com.pingidentity.labs.rapport.StateManager;
import com.pingidentity.labs.rapport.TransactionMessage;

import com.github.dwaite.cyborg.electrode.CborException;
import com.github.dwaite.cyborg.electrode.impl.CborDataInput;
import com.github.dwaite.cyborg.electrode.impl.CborOutput;

public class DTVAStateManager implements StateManager<StateImpl, DTVATransaction> {
	private final Logger log = LoggerFactory.getLogger(DTVAStateManager.class);
	private static final boolean DEBUG = true;

	@Override
	public StateImpl createInitialState(List<? extends Peer> addresses, JsonValue constitutionValue) {
		Objects.requireNonNull(addresses);
		Objects.requireNonNull(constitutionValue);
		if (!(constitutionValue instanceof JsonObject)) {
			return null;
		}
		JsonObject constitutionObject = (JsonObject) constitutionValue;
		Constitution constitution = ConstitutionImpl.fromJSON(addresses, constitutionObject);
		return new StateImpl(constitution);
	}

	@Override
	public byte[] serializeTransactions(List<? extends DTVATransaction> transactions) {
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos)) {
			for (DTVATransaction transaction: transactions) {
				transaction.writeExternal(dos);
			}
			dos.close();
			return bos.toByteArray();
		} catch (IOException e) {
			log.error("Unexpected issue while serializing transcations", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<DTVATransaction> parseTransactions(byte[] transactionData) {
		try {
			List<DTVATransaction> results = new ArrayList<>();
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(transactionData));
			while (dis.available() > 0) {
				dis.mark(10);
				CborDataInput input = new CborDataInput(dis);
				if (input.readStartArray() != 2) {
					dis.reset();
					throw new CborException("expected transaction to be a two-element array");
				}
				int type = input.readInteger();
				dis.reset();

				DTVATransaction tx;
				switch(DTVATransaction.Type.values()[type]) {
				case RegisterValidityKey:
					tx = new ValidityKeyRegistrationTransaction(dis);
					break;
				case UpdateInteractivity:
					tx = new ValidityKeyInteractivityTransaction(dis);
					break;
				case Invalidate:
					tx = new ValidityKeyInvalidationTransaction(dis);
					break;
				case RegisterIssuer:
					tx = new IssuerRegistrationTransaction(dis);
					break;
				default:
					throw new IOException("Unrecognized transaction code " + type);
				}
				results.add(tx);
			}
			return results;
		}
		catch (IOException | ClassNotFoundException | CborException e) {
			log.error("Unexpected issue while parsing transcations", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public StateImpl handleTransactions(StateImpl originalState, List<? extends TransactionMessage<DTVATransaction>> transactions) {
		StateImpl originalStateImpl = (StateImpl) originalState;
		assert(!transactions.isEmpty());
		Instant earliestInstant = transactions.iterator().next().getConsensusEstablishedTime();
		if (DEBUG) {
			Instant earliestInTransactions = transactions.stream().map(TransactionMessage::getConsensusEstablishedTime).min((o1, o2) -> o1.compareTo(o2)).get();
			if (earliestInTransactions.isBefore(earliestInstant)){
				log.warn("Messages are not in consensus time order, for now going with earliest transaction");
				earliestInstant = earliestInTransactions;
			}
		}
		ConcurrentSkipListMap<ValidityKey, ValidityKeyRecord> newSessions = cleanUpSessions(originalStateImpl.periods, earliestInstant);
		List<IssuerImpl> newIssuers = new ArrayList<>(originalStateImpl.issuerImpls);
		
		for (TransactionMessage<DTVATransaction> tx : transactions) {
			DTVATransaction transaction = tx.getTransaction();
			switch (transaction.getType()) {
			case RegisterValidityKey:
				ValidityKeyRegistrationTransaction creation = (ValidityKeyRegistrationTransaction)transaction;
				log.debug("creation received for " + creation.getKey());
				ValidityKeyRecord session = new ValidityKeyRecord(
						creation.getKey().getHardExpiryAt(),
						tx.getConsensusEstablishedTime(),
						creation.getKey().getInteractivityTimeout());
				ValidityKeyRecord oldSession = newSessions.putIfAbsent(creation.getKey(), session);
				if (oldSession != null) {
					log.error("Transaction received to create a session which already existed. Ignoring. DebugState may now " +
							"be inconsistent");
					continue;
				}
				break;
			case UpdateInteractivity:
				ValidityKeyInteractivityTransaction update = (ValidityKeyInteractivityTransaction)transaction;
				log.debug("update received for " + update.getKey());

				synchronized(this) {
					session = newSessions.get(update.getKey());
					if (session == null) {
						log.debug("Transaction received to update a session which does not exist. This may mean that by " +
								"the time it was received, the session had lapsed and been cleaned up. Ignoring.");
						continue;
					}
					session = session.updated(tx.getConsensusEstablishedTime()).orElse(null);
					if (session == null) {
						log.debug("Update received for session which has been invalidated or expired. Ignoring");
						continue;
					}
					newSessions.put(update.getKey(), session);
				}
				break;
			case Invalidate:
				ValidityKeyInvalidationTransaction invalidation = (ValidityKeyInvalidationTransaction) transaction;
				log.debug("invalidation received for " + invalidation.getKey());
				synchronized(this) {
					session = newSessions.get(invalidation.getKey());
					if (session == null) {
						log.debug("Invalidation received for session which does not exist. Ignoring");
						continue;
					}
					session = session.invalidated(tx.getConsensusEstablishedTime()).orElse(null);
					if (session == null) {
						log.debug("Session invalidation for an already invalidated or expired session. Ignoring");
						continue;
					}
					newSessions.put(invalidation.getKey(), session);
				}
				break;
			case RegisterIssuer:
				IssuerRegistrationTransaction register = (IssuerRegistrationTransaction) transaction;
				log.debug("register issuer received for " + register.getIssuerName());
				boolean containsIssuer = 
						newIssuers.stream().anyMatch((issuer) -> issuer.getIssuerName().equals(register.getIssuerName()));
				if (!containsIssuer) {
					// FIXME need participant identifier
					IssuerImpl issuerImpl = new IssuerImpl(register.getIssuerName(), 0, newIssuers.size());
					log.info("registering new issuer" + issuerImpl);
					newIssuers = new ArrayList<>(newIssuers);
					newIssuers.add(issuerImpl);
				}
				break;
			default:
				// FIXME
				throw new UnsupportedOperationException();
			}
		}
		return new StateImpl(originalState.getConstitution(), newSessions, newIssuers);
	}

	private ConcurrentSkipListMap<ValidityKey, ValidityKeyRecord> cleanUpSessions( NavigableMap<ValidityKey, ValidityKeyRecord> sessions, Instant timeCreated) {
		log.debug("Attempting to clean up expired sessions");
		return new ConcurrentSkipListMap<>(sessions.tailMap(ValidityKey.smallestAtInstant(timeCreated)));
	}

	@Override
	public StateImpl deserializeState(DataInput di) throws IOException {
		try {
			ConstitutionStateManager constitutionState = new ConstitutionStateManager();
			CborDataInput input = new CborDataInput(di);
			
	//		if (dis.readLong() != DSM_STATE_VERSION) {
	//			throw new IOException("Unknown DSM StateImpl version");
	//		}
			if (input.readStartArray() != 3) {
				throw new CborException("expected three element array at root of state");
			}
			// safe, since we haven't peeked the next cbor data type
			// when returned, will be advanced past the first array element
			ConstitutionImpl constitutionImpl = constitutionState.deserializeState(di);
			
			List<IssuerImpl> issuerImpls = new ArrayList<>();
			int issuerCount = input.readStartArray();
			for (int i = 0; i< issuerCount; i++) {
				int elementCount = input.readStartArray();
				if (elementCount != 2) {
					throw new CborException("expected two element array for each issuer name entry");
				}
				String issuerName = input.readText();
				int participant = input.readInteger();
				IssuerImpl issuerImpl = new IssuerImpl(issuerName, participant, i);
				issuerImpls.add(issuerImpl);
			}

			int validityKeyCount = input.readStartArray();
			try {
				ConcurrentSkipListMap<ValidityKey, ValidityKeyRecord> periods = new ConcurrentSkipListMap<>();
				IntStream.range(0, validityKeyCount).forEach((idx)-> {
					try {
						ValidityKey key = new ValidityKey(di);
						Instant lastActivityAt = Instant.ofEpochSecond(input.readLong());
						Optional<Instant> invalidatedAt;
						switch(input.peek().getInitialByte().getLogicalType()) {
						case INTEGRAL:
							invalidatedAt = Optional.of(Instant.ofEpochSecond(input.readLong()));
							break;
						case NULL:
							input.readNull();
							invalidatedAt = Optional.empty();
						default:
							throw new CborException("expected invalidation time for validity key to be represented by either an integer, or null if still valid")	;
						}
						ValidityKeyRecord record = 
								new ValidityKeyRecord(key.getHardExpiryAt(),
										lastActivityAt,
										key.getInteractivityTimeout());
						if (invalidatedAt.isPresent()) {
							record = record.invalidated(invalidatedAt.get()).orElseThrow(()-> new RuntimeException("Issue recreating invalidated record"));
						}
						periods.put(key, record);
					}
					catch (IOException | CborException e) {
						throw new IllegalStateException(e);
					}
				});
				StateImpl stateImpl = new StateImpl(constitutionImpl, periods , issuerImpls);
				return stateImpl;
			}
			catch (IllegalStateException e) {
				Throwable inner = e.getCause();
				if (inner instanceof IOException)
					throw (IOException) inner;
				throw e;
			}
		}
		catch (CborException e) {
			throw new IOException("Error with CBOR data", e);
		}
	}

	@Override
	public void serializeState(StateImpl stateImpl, DataOutput dout) throws IOException {
		try {
			CborOutput output = new CborOutput(dout);
			ConstitutionStateManager constitutionState = new ConstitutionStateManager();
			output.writeStartArray(3);
			constitutionState.serializeState((ConstitutionImpl)stateImpl.getConstitution(), dout);
			output.writeStartArray(stateImpl.getIssuers().size());
			stateImpl.getIssuers().forEach((issuer) -> {
				try {
					output.writeStartArray(2)
						.writeText(issuer.getIssuerName())
						.writeLong(issuer.getIssuingParticipant());
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
			Map<ValidityKey, ValidityKeyRecord> records = ((StateImpl)stateImpl).periods;
			output.writeStartArray(records.size());
			records.entrySet().forEach((kv) -> {
				try {
					ValidityKey key = kv.getKey();
					output.writeStartArray(3);
					key.writeExternal(dout);
					output.writeLong(kv.getValue().getLastActivityAt().getEpochSecond());
					Optional<Instant> invalidatedAt = kv.getValue().getInvalidatedAt();
					if (invalidatedAt.isPresent()) {
						output.writeLong(invalidatedAt.get().getEpochSecond());
					}
					else {
						output.writeNull();
					}
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
		}
		catch (IllegalStateException e) {
			Throwable inner = e.getCause();
			if (inner instanceof IOException)
				throw (IOException) inner;
			throw e;
		}
	}
}
