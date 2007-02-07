package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.util.Collection;
import java.util.Set;

import org.red5.server.adapter.IApplication;

/**
 * The client object represents a single client. One client may have multiple
 * connections to different scopes on the same host. In some ways the client
 * object is like a HTTP session. You can create IClient objects with
 * {@link IClientRegistry#newClient(Object[])}
 * 
 * 
 * NOTE: I removed session, since client serves the same purpose as a client
 * with attributes
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IClient extends IAttributeStore, IFlowControllable {

	/**
	 * The key used to store the client object in a http session.
	 */
	public static final String ID = "red5.client";

	/**
	 * Get the unique ID for this client.  This will be generated by the server
	 * if not passed upon connection from client-side Flex/Flash app. To assign a custom ID to the client use
	 * <code>params</code> object of
	 * {@link IApplication#appConnect(IConnection, Object[])} method, that
	 * contains 2nd all the rest values you pass to
	 * <code>NetConnection.connect</code> method.
	 * 
	 * Example:
	 * 
	 * At client side:
	 * <code>NetConnection.connect( "http://localhost/killerapp/", "user123" );</code>
	 * 
	 * then at server side: <code>
	 * public boolean appConnect( IConnection connection, Object[] params ){<br/>
	 * 		try {
	 * 			connection.getClient().setStreamId( params[0] );
	 * 		} catch(Exception e){<br/>
	 * 			log.error(e);
	 * 		}
	 * }
	 * </code>
	 * 
	 * @return client id
	 */
	public String getId();

	/**
	 * Get the creation time for this client object.
	 * 
	 * @return Creation time in milliseconds
	 */
	public long getCreationTime();

	/**
	 * Get a set of scopes the client is connected to.
	 * 
	 * @return Set of scopes
	 */
	public Collection<IScope> getScopes();

	/**
	 * Get a set of connections.
	 * 
	 * @return Set of connections
	 */
	public Set<IConnection> getConnections();

	/**
	 * Get a set of connections of a given scope.
	 * 
	 * @param scope
	 * 			scope to get connections for
	 * @return Set of connections to the passed scope
	 */
	public Set<IConnection> getConnections(IScope scope);

	/**
	 * Closes all the connections.
	 */
	public void disconnect();

}