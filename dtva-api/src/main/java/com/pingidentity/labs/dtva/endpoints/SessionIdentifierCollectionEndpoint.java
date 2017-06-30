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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.github.dwaite.problemdetails.ProblemDetails;
import com.github.dwaite.problemdetails.ProblemType;
import com.pingidentity.labs.dtva.application.DTVACoordinator;
import com.pingidentity.labs.dtva.application.Issuer;
import com.pingidentity.labs.dtva.application.State;
import com.pingidentity.labs.dtva.application.ValidityKey;
import com.pingidentity.labs.dtva.application.ValidityKeyView;
import com.pingidentity.labs.dtva.endpoints.util.Constants;
import com.pingidentity.labs.dtva.endpoints.util.GraceValidityKeyView;
import com.pingidentity.labs.dtva.endpoints.util.SessionIdentifier;
import com.pingidentity.labs.dtva.endpoints.util.SessionIdentifierView;
import com.pingidentity.labs.dtva.endpoints.util.ValidityKeyCreationRequest;

@Produces({MediaType.APPLICATION_JSON, Constants.APPLICATION_CBOR})
@Consumes({MediaType.APPLICATION_JSON, Constants.APPLICATION_CBOR})
@Path("/validity")
public class SessionIdentifierCollectionEndpoint {
	private @NotNull DTVACoordinator platform;
	public SessionIdentifierCollectionEndpoint(@NotNull DTVACoordinator platform) {
		this.platform = platform;
	}
	
	@POST
	public Response createSession(ValidityKeyCreationRequest request) throws IOException {
		Instant now = Instant.now();
		Optional<Issuer> issuer = platform.withStateEvaluatedAtTime(now, (State state) ->
			state.getIssuerByName(request.getIssuerName()));
		if (!issuer.isPresent()) {
			return ProblemType.forHttpStatus(Status.BAD_REQUEST)
					.builder()
					.detail("Issuer name is not currently known")
					.customAttributes((jsonObj) -> 
						jsonObj.add("missingIssuer", request.getIssuerName()))
					.build();
		}
		// FIXME consensus grace is now part of the identifier
		Optional<Duration> grace = platform.getConsensusGraceSpan();
		Optional<Instant> graceExpiryAt = grace.map(now::plus);
		ValidityKey key = platform.sendValidityKeyRegistration(request.getHardExpiryAt(), issuer.get(), request.getInteractivityTimeout());
		System.out.println("Created transaction");
		SessionIdentifier sid = new SessionIdentifier(key, graceExpiryAt.orElse(null));
		URI path = URI.create("/validity/" + sid.toStringIdentifier());
		
		return platform.withStateEvaluatedAtTime(now, (State state) -> {
			return state.viewValidityKey(key).filter(ValidityKeyView::isActive).map((view) ->
				Response
					.created(path)
					.entity(new SessionIdentifierView(view, sid))
					.build())
			.orElse(
					Response
					.accepted()
					.location(path)
					.build());
		});
	}
	
	@GET
	public Response findSession(@QueryParam("sid") SessionIdentifier sid, @QueryParam("iss") String iss) {
		Instant now = Instant.now();
		Optional<ValidityKeyView> oview = platform.withStateEvaluatedAtTime(now, (State state) -> state.viewValidityKey(sid.getKey()));
		if(oview.isPresent()) {
			ValidityKeyView view = oview.get();
			if (!view.getIssuer().getIssuerName().equals(iss)) {
				return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.BAD_REQUEST))
						.detail("session identifier is not bound to the supplied issuer name")
						.build();
			}
		}
		
		boolean isInGrace = sid.getConsensusGrace().map( (grace) -> grace.isAfter(now)).orElse(false);
		if (!isInGrace) {
			return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.NOT_FOUND))
					.detail("Session identifier is not known")
					.build();
		}
		URI location = URI.create("./" + sid.toStringIdentifier());
		return ProblemDetails.ofType(ProblemType.forHttpStatus(Constants.PERMANENT_REDIRECT))
				.detail("Session identifier details are available at the provided URL")
				.customAttributes( (builder) -> builder.add("location", location.toString()) )
				.builder()
				.location(location)
				.build();
	}

	@GET
	@Path("{sid}")
	public Response getSession(@PathParam("sid") SessionIdentifier sid) {
		Instant now = Instant.now();
		Optional<ValidityKeyView> view = platform.withStateEvaluatedAtTime(now, (State state) -> state.viewValidityKey(sid.getKey()));
		if (view.isPresent()) {
			return Response.ok(new SessionIdentifierView(view.get(), sid)).build();
		}
		boolean isInGrace = sid.isInGrace(now);
		if (!isInGrace) {
			return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.NOT_FOUND))
					.detail("Session identifier is not known")
					.build();
		}
		try {
			Issuer issuer = platform.withStateEvaluatedAtTime(now, (State state) -> state.getIssuers().get(sid.getKey().getIssuerIndex()));
			SessionIdentifierView sessionView = new SessionIdentifierView(new GraceValidityKeyView(now, sid, issuer), sid);
			return Response.ok(sessionView).build();
		}
		catch (IndexOutOfBoundsException e) {
			return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.BAD_REQUEST))
					.detail("Session identifier was issued by an unknown issuer.")
					.build();
		}
	}
	
	@POST
	@Path("{sid}")
	public Response updateSession(@PathParam("sid") SessionIdentifier sid, @FormParam("interactivity_detected") Boolean interactivityDetected ) {
		ValidityKey key = sid.getKey();
		platform.sendValidityKeyInteractivity(key);
		Instant now = Instant.now();
		Optional<ValidityKeyView> view = platform.withStateEvaluatedAtTime(now, (State state) -> state.viewValidityKey(key));
		if (view.isPresent()) {
			if (view.get().isActive())
				return Response.ok(new SessionIdentifierView(view.get(), sid)).build();
			if (view.get().isExpired())
				return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.GONE))
						.detail("sid has already expired")
						.customAttributes((builder) ->
						builder.add("sid", sid.toStringIdentifier())
						.add("hard_expiry_at", view.get().getHardExpiryAt().toEpochMilli()))
						.build();
			if (view.get().isInvalidated())
				return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.CONFLICT))
						.detail("sid has been invalidated")
						.customAttributes((builder) ->
						builder.add("sid", sid.toStringIdentifier())
						.add("invalidated_at", view.get().getInvalidatedAt().get().toEpochMilli()))
						.build();
			throw new IllegalStateException("view is neither active, expired or invalidated");
		}
		
		if (sid.isInGrace(now)) {
			Issuer iss = platform.withStateEvaluatedAtTime(now, (State state) -> state.getIssuers().get(key.getIssuerIndex()));
			return Response.accepted(new SessionIdentifierView(new GraceValidityKeyView(now, sid, iss), sid)).build();
		}
		
		return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.NOT_FOUND)).build();
	}
	
	@DELETE
	public Response deleteSession(@QueryParam("sid") SessionIdentifier sid, @QueryParam("iss") String issuerName) {
		Instant now = Instant.now();
		ValidityKey key = sid.getKey();
		Issuer issuer = platform.withStateEvaluatedAtTime(now, (State state) -> state.getIssuers().get(key.getIssuerIndex()));
		if (issuer.getIssuerName().equals(issuerName)) {
			return deleteSession(sid);
		}
		return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.BAD_REQUEST))
				.detail("session identifier is not bound to the supplied issuer name")
				.build();
	}

	@DELETE
	@Path("{sid}")
	public Response deleteSession(@PathParam("sid") SessionIdentifier sid) {
		ValidityKey key = sid.getKey();
		platform.sendValidityKeyInvalidation(key);
		Instant now = Instant.now();
		Optional<ValidityKeyView> view = platform.withStateEvaluatedAtTime(now, (State state) -> state.viewValidityKey(key));
		if (view.isPresent()) {
			if (view.get().isExpired()) {
				return Response.ok(new SessionIdentifierView(view.get(), sid)).build();
			}
			return Response.accepted(new SessionIdentifierView(view.get(), sid)).build();
		}
		if (sid.isInGrace(now)) {
			Issuer iss = platform.withStateEvaluatedAtTime(now, (State state) -> state.getIssuers().get(key.getIssuerIndex()));
			return Response.accepted(new SessionIdentifierView(new GraceValidityKeyView(now, sid, iss), sid)).build();
		}
		return ProblemDetails.ofType(ProblemType.forHttpStatus(Status.NOT_FOUND)).build();
	}
}