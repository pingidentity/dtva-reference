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
package com.pingidentity.labs.dtva.server;

import java.io.File;
import java.net.InetSocketAddress;

import javax.json.JsonObject;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;

import com.pingidentity.labs.dtva.application.DTVACoordinator;
import com.pingidentity.labs.dtva.application.impl.StateImpl;
import com.pingidentity.labs.dtva.application.transactions.DTVATransaction;
import com.pingidentity.labs.rapport.Coordinator;

/**
 * Rapport application (building on the DTVA app) to use a HTTP API-based coordinator
 */
public class WebAPIApplication extends com.pingidentity.labs.dtva.application.DTVABaseApplication {

	@Override
	public void createInteractor(Coordinator<StateImpl, DTVATransaction> coordinator) {
		DTVACoordinator dtvaCoordinator = super.wrapCoordinator(coordinator);
		InetSocketAddress webAPI = getLocalWebAPI(dtvaCoordinator);
		
		Server server = new Server(webAPI.getPort());

	    HttpConfiguration httpConfig = new HttpConfiguration();
	    // set forwarding support on
	    httpConfig.addCustomizer( new org.eclipse.jetty.server.ForwardedRequestCustomizer() );
	    HttpConnectionFactory connectionFactory = new HttpConnectionFactory( httpConfig );
	    ServerConnector connector = new ServerConnector(server, connectionFactory);
	    connector.setPort( webAPI.getPort() );
	    server.setConnectors( new ServerConnector[] { connector } );
	    HandlerCollection handlers = server.getChildHandlerByClass(HandlerCollection.class);
        if (handlers == null) 
        {
            handlers = new HandlerCollection();
            server.setHandler(handlers);
        }
		ContextHandlerCollection contexts = handlers.getChildHandlerByClass(ContextHandlerCollection.class);
		if (contexts == null) {
			contexts = new ContextHandlerCollection();
			Handler[] existing = contexts.getChildHandlers();
			Handler[] newer = new Handler[existing.length + 1];
			newer[0] = contexts;
			System.arraycopy(existing, 0, newer, 1, existing.length);
		}
		
		File warFile = new File("./lib/dtva-api.war");
		if (!warFile.exists()) {
			warFile = new File("./build/dtva-api.war");
		}
		WebAppContext ctx = new WebAppContext(contexts, warFile.getAbsolutePath(), "/");
		ctx.setAttribute("com.pingidentity.labs.dtva.state.PlatformInstance", dtvaCoordinator);
		server.setHandler(ctx);
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	protected InetSocketAddress getLocalWebAPI(DTVACoordinator coordinator) {
		JsonObject localConfig = (JsonObject) coordinator.getLocalConfiguration();
		if (localConfig != null) {
			String webAPIString = localConfig.getString("webAPI");
			if (webAPIString == null) {
				throw new IllegalArgumentException("No 'webAPI' specified in local configuration");
			}
			InetSocketAddress webAPI = 
					SocketAddressAdapter.unmarshalSocket(webAPIString);
			return webAPI;
		}
		return null;
	}
}
