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
package com.pingidentity.labs.dtva.application;

import java.time.Duration;
import java.util.List;

/** 
 * Represents rules for processing that all participants must agree upon, and thus part of the state
 * of the distributed system
 */
public interface Constitution {
	/** List of participants and their properties within the system. 
	 * 
	 * A {@link Participant} contains an identifier used for distinguishing them (likely cryptographic)
	 * and their access permissions.
	 */
	List<? extends Participant> getParticipants();
	
	/**
	 * The maximum duration past the time of initial consensus on a validity key that the key may be set
	 * to expire. Past this time, the registration will fail.
	 */
	Duration getMaxHardExpiryIn();
}