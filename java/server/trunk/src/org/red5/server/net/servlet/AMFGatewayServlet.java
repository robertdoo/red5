package org.red5.server.net.servlet;

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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.remoting.IRemotingConnection;
import org.red5.server.net.remoting.RemotingConnection;
import org.red5.server.net.remoting.codec.RemotingCodecFactory;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that handles remoting requests.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public class AMFGatewayServlet extends HttpServlet {

	private static final long serialVersionUID = 7174018823796785619L;

	/**
	 * Logger
	 */
	protected static Logger log = LoggerFactory.getLogger(AMFGatewayServlet.class);

	/**
	 * AMF MIME type
	 */
	public static final String APPLICATION_AMF = "application/x-amf";

	/**
	 * Web app context
	 */
	protected WebApplicationContext webAppCtx;

	/**
	 * Web context
	 */
	protected IContext webContext;

	/**
	 * Remoting codec factory
	 */
	protected RemotingCodecFactory codecFactory;

	/**
	 * Request attribute holding the Red5 connection object
	 */
	private static final String CONNECTION = "red5.remotingConnection";
	
	/** {@inheritDoc} */
	@Override
	public void init() throws ServletException {
		webAppCtx = WebApplicationContextUtils
				.getWebApplicationContext(getServletContext());
		if (webAppCtx == null) {
			webAppCtx = (WebApplicationContext) getServletContext()
					.getAttribute(
							WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		if (webAppCtx != null) {
			webContext = (IContext) webAppCtx.getBean("web.context");
			codecFactory = (RemotingCodecFactory) webAppCtx
					.getBean("remotingCodecFactory");
		} else {
			log.debug("No web context");
		}
	}

	/** {@inheritDoc} */
	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.debug("Servicing Request");
		if (log.isDebugEnabled()) {
			log.debug("Remoting request" + req.getContextPath() + ' '
					+ req.getServletPath());
		}
		if (req.getContentType() != null
				&& req.getContentType().equals(APPLICATION_AMF)) {
			serviceAMF(req, resp);
		} else {
			resp.getWriter().write("Red5 : Remoting Gateway");
		}
	}

	/**
	 * Works out AMF request
	 * 
	 * @param req
	 *            Request
	 * @param resp
	 *            Response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             I/O exception
	 */
	protected void serviceAMF(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.debug("Servicing AMF");
		try {
			RemotingPacket packet = decodeRequest(req);
			if (packet == null) {
				log.error("Packet should not be null");
				return;
			}
			// Provide a valid IConnection in the Red5 object
			IScope scope = webContext.resolveScope(packet.getScopePath());
			IRemotingConnection conn = new RemotingConnection(req, scope, packet);
			// Make sure the connection object isn't garbage collected
			req.setAttribute(CONNECTION, conn);
			try {
				Red5.setConnectionLocal(conn);
				handleRemotingPacket(req, packet);
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.setContentType(APPLICATION_AMF);
				sendResponse(resp, packet);
			} finally {
				req.removeAttribute(CONNECTION);
			}
		} catch (Exception e) {
			log.error("Error handling remoting call", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Decode request
	 * 
	 * @param req
	 *            Request
	 * @return Remoting packet
	 * @throws Exception
	 *             General exception
	 */
	protected RemotingPacket decodeRequest(HttpServletRequest req)
			throws Exception {
		log.debug("Decoding request");
		ByteBuffer reqBuffer = ByteBuffer.allocate(req.getContentLength());
		ServletUtils.copy(req.getInputStream(), reqBuffer.asOutputStream());
		reqBuffer.flip();
		RemotingPacket packet = (RemotingPacket) codecFactory
				.getSimpleDecoder().decode(null, reqBuffer);
		String path = req.getContextPath();
		if (path == null) {
			path = "";
		}
		if (req.getPathInfo() != null) {
			path += req.getPathInfo();
		}
		// check for header path, this is used by the AMF tunnel servlet
		String headerPath = req.getHeader("Tunnel-request");
		// it is only used if the path is set to root
		if (headerPath != null && path.length() < 1) {
			path = headerPath;
		}
		if (path.length() > 0 && path.charAt(0) == '/') {
			path = path.substring(1);
		}
		log.debug("Path: " + path + " Scope path: " + packet.getScopePath());
		packet.setScopePath(path);
		reqBuffer.release();
		reqBuffer = null;
		return packet;
	}

	/**
	 * Handles AMF request by making calls
	 * 
	 * @param req
	 *            Request
	 * @param message
	 *            Remoting packet
	 * @return <code>true</code> on success
	 */
	protected boolean handleRemotingPacket(HttpServletRequest req,
			RemotingPacket message) {
		log.debug("Handling remoting packet");
		IScope scope = webContext.resolveScope(message.getScopePath());
		for (RemotingCall call : message.getCalls()) {
			webContext.getServiceInvoker().invoke(call, scope);
		}
		return true;
	}

	/**
	 * Sends response to client
	 * 
	 * @param resp
	 *            Response
	 * @param packet
	 *            Remoting packet
	 * @throws Exception
	 *             General exception
	 */
	protected void sendResponse(HttpServletResponse resp, RemotingPacket packet)
			throws Exception {
		log.debug("Sending response");
		ByteBuffer respBuffer = codecFactory.getSimpleEncoder().encode(null,
				packet);
		final ServletOutputStream out = resp.getOutputStream();
		resp.setContentLength(respBuffer.limit());
		ServletUtils.copy(respBuffer.asInputStream(), out);
		out.flush();
		out.close();
		respBuffer.release();
		respBuffer = null;
	}

}