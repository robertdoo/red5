package org.red5.server.net.impl;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IMapping;
import org.red5.server.api.IScope;
import org.red5.server.api.impl.Connection;
import org.red5.server.context.GlobalContext;
import org.red5.server.net.AccessDeniedException;
import org.red5.server.net.ClientNotFoundException;
import org.red5.server.net.HostNotFoundException;
import org.red5.server.net.IConnectionHandler;
import org.red5.server.net.ScopeNotFoundException;

public  class ConnectionHandler implements IConnectionHandler {
	
	private GlobalContext global;
	private IMapping mapping;
	
	public boolean connect(IConnection conn) throws AccessDeniedException {
		return false;
	}

	public void disconnect(IConnection conn) {
		// TODO Auto-generated method stub
		
	}

	public IClient lookupClient(String sessionId) throws ClientNotFoundException {
		// find the client in a map
		return null;
	}

	public IClient newClient(String host) throws HostNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	public IScope lookupScope(IClient client, String contextPath) throws ScopeNotFoundException {
		//global.hasHostContext()
		// lookup a scope
		return null;
	}

	
}