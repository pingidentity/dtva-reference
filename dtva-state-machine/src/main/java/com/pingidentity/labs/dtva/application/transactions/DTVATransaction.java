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

import java.io.DataOutput;
import java.io.IOException;

public interface DTVATransaction {
	public enum Type {
		RegisterIssuer,
		RegisterValidityKey,
		UpdateInteractivity,
		Invalidate
	}
	
	Type getType();	
	void writeExternal(DataOutput dos) throws IOException;

}
