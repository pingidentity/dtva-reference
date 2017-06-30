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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import com.pingidentity.labs.dtva.application.ValidityKey;
import static org.hamcrest.CoreMatchers.*;

public class SessionIdentifierTest {

	@Test
	public void testSerialization() throws IOException {
		Instant now = Instant.now();
		Instant hardExpiryAt = now.plus(8, ChronoUnit.HOURS);
		long issuerIndex = 10;
		Optional<Duration> interactivityTimeout = Optional.of(Duration.ofMinutes(15));
		long nonce = new Random().nextLong();
		ValidityKey key = new ValidityKey(hardExpiryAt, issuerIndex, interactivityTimeout, nonce);
		Instant consensusGrace = now.plus(1, ChronoUnit.MINUTES);
		SessionIdentifier sid = new SessionIdentifier(key, consensusGrace);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		sid.writeExternal(dos);
		dos.close();
		byte sidData[] = baos.toByteArray();
		assertThat(sidData, is(notNullValue()));
		
		ByteArrayInputStream bais = new ByteArrayInputStream(sidData);
		DataInputStream dis = new DataInputStream(bais);
		SessionIdentifier deserializedSid = new SessionIdentifier(dis);
		ValidityKey deserializedKey = deserializedSid.getKey();
		assertThat(deserializedKey, is(notNullValue()));
		assertThat(deserializedKey.getHardExpiryAt(), is(equalTo(key.getHardExpiryAt())));
		assertThat(deserializedKey.getInteractivityTimeout(), is(equalTo(key.getInteractivityTimeout())));
		assertThat(deserializedKey.getIssuerIndex(), is(equalTo(key.getIssuerIndex())));
		assertThat(deserializedKey.getNonce(), is(equalTo(key.getNonce())));

		assertThat(deserializedSid.getConsensusGrace(), is(equalTo(sid.getConsensusGrace())));
		
	}

}
