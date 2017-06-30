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
package com.pingidentity.labs.dtva.endpoints;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.github.dwaite.problemdetails.ProblemDetails;
import com.github.dwaite.problemdetails.ProblemType;
import com.pingidentity.labs.dtva.application.DTVACoordinator;
import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.Participant;
import com.pingidentity.labs.dtva.application.State;

/**
 *  allow pre-registration and debug introspection of issuer names
 */
@Path("/issuer")
public class IssuerNameCollectionEndpoint {
	private @NotNull DTVACoordinator platform;
	public IssuerNameCollectionEndpoint(@NotNull DTVACoordinator platform) {
		this.platform = platform;
	}
	
	@GET
	public Response getIssuers() {
		Instant now = Instant.now();
		List<Issuer> issuers = platform.withStateEvaluatedAtTime(now, State::getIssuers);
		return Response
				.ok(new GenericEntity<List<Issuer>>(issuers) {}, MediaType.APPLICATION_JSON_TYPE)
				.cacheControl(CacheControl.valueOf("no-cache, no-store, must-revalidate"))
				.header("Pragma", "no-cache")
				.expires(Date.from(now.plus(Duration.ofMinutes(1))))
				.build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addIssuer(@FormParam("iss") String issuerName) {
		Instant now = Instant.now();
		Participant self = platform.getSelf();
		Optional<Issuer> issuer = platform.withStateEvaluatedAtTime(now, (State state) -> state.getIssuerByName(issuerName));
		if (issuer.isPresent()) {
			return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.CONFLICT))
					.detail("Issuer is already registered")
					.customAttributes((builder) -> builder.add("iss", issuerName))
					.build();
		}
		platform.sendIssuerRegistration(issuerName);
		// FIXME race - need to verify that the local participant is the one the name is registered to. If
		// not, they'll get a conflict here too.
		issuer = platform.withStateEvaluatedAtTime(now, (State state) -> state.getIssuerByName(issuerName));
		if (issuer.isPresent()) {
			Participant issuingParticipant = issuer.get().getIssuingParticipant();
			if (issuingParticipant.equals(self)) {
				return Response.ok().build();
			}
			else
				return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.CONFLICT))
						.detail("issuer registered to a different participant")
						.customAttributes((builder) ->
						builder.add("iss", issuerName)
						.add("participant", issuingParticipant.getDisplayName().get()))
						.build();
		} else {
			return Response.accepted().location(URI.create("/issuer")).build();
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addIssuer(JsonObject doc) {
		String iss = doc.getString("iss");
		if (iss == null) {
			return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.BAD_REQUEST))
					.detail("issuer ('iss') not specified to register a new issuer")
					.build();
		}
		return addIssuer(iss);
	}

}
