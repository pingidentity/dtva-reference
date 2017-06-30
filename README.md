# Distributed Token Validity API

This project and sub-projects implement a REST API to track token lifetimes, from creation to expiry or invalidation by their issuing party. It is based on The Distributed Token Validity API draft submitted to the OpenID Foundation Connect Working Group [here](https://bitbucket.org/openid/connect/src/4dc66f0077597e08f9758379a87fb5f9be06359c/distributed-token-validity-api.txt?at=default&fileviewer=file-view-default)

## Project Structure

This project is build on the [Rapport](https://github.com/pingidentity/rapport) library, which initially supports either using the [Swirlds Hashgraph](https://www.swirlds.com/) based back-end, or a purely local back-end. The default build uses this second "lonely" back-end by default to avoid the dependency on shipping
the Swirlds libraries.

The project is divided into three subprojects:

- **dtva-state-machine** The state management and basic usage of a rapport-based DTVA participant. This project could be used as a Rapport application, participating in consensus but exposing no API or logic that reads or attempts to modify the system state.
- **dtva-api** A JAX-RS based HTTP API implementation which expands on the state machine to add an interface for reading and manipulation of state. This application requires some special deployment considerations to run
- **dtva-server** A Rapport application which embeds Eclipse Jetty as a web server, launching the `dtva-api` while exposing the state machine details it needs to work.

## Running

The `dtva-server` project exposes a `runWithConstitution` task which will start up a single local participant based on the "lonely" rapport back-end. The configuration for this participant is included in `src/main/dist/constitution.json`