/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.api;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.red5.server.adapter.IApplication;
import org.red5.server.api.scope.IScope;

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
public interface IClient extends IAttributeStore {

	/**
	 * The key used to store the client object in a http session.
	 */
	public static final String ID = "red5.client";

	/**
	 * Sets the clients id
	 * @param id client id
	 */
	public void setId(String id);

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
	 * 			connection.getClient().setId( (String) params[0] );
	 * 		} catch(Exception e){<br/>
	 * 			log.error("{}", e);
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
	 * @param scope scope to get connections for
	 * @return Set of connections to the passed scope
	 */
	public Set<IConnection> getConnections(IScope scope);

	/**
	 * Closes all the connections.
	 */
	public void disconnect();

	/**
	 * Set the permissions for this client in a given context.
	 * 
	 * @param conn Connection specifying the context to set the permissions for. 
	 * @param permissions Permissions the client has in this context or <code>null</code> for no permissions.
	 */
	public void setPermissions(IConnection conn, Collection<String> permissions);

	/**
	 * Return the permissions in a given context. 
	 * 
	 * @param conn Connection specifying the context to get the permissions for.
	 * @return Permission names.
	 */
	public Collection<String> getPermissions(IConnection conn);

	/**
	 * Check if the client has a permission in the given context.
	 * 
	 * @param conn Connection specifying the context to check the permissions for.
	 * @param permissionName Name of the permission to check.
	 * @return <code>true</code> if the client has the permission, otherwise <code>false</code>
	 */
	public boolean hasPermission(IConnection conn, String permissionName);

	/**
	 * Performs a bandwidth checking routine.
	 * Information may be found here: http://www.adobe.com/devnet/flashmediaserver/articles/dynamic_stream_switching_04.html
	 */
	public void checkBandwidth();

	/**
	 * Performs a bandwidth checking callback for the client.
	 * Information may be found here: http://www.adobe.com/devnet/flashmediaserver/articles/dynamic_stream_switching_04.html
	 */
	public Map<String, Object> checkBandwidthUp(Object[] params);

	/**
	 * Returns whether or not a bandwidth check has been requested.
	 * @return true if requested and false otherwise
	 */
	public boolean isBandwidthChecked();

}
