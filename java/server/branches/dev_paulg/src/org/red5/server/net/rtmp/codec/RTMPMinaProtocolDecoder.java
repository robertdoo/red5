package org.red5.server.net.rtmp.codec;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.util.Iterator;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.red5.server.net.protocol.ProtocolState;

public class RTMPMinaProtocolDecoder extends RTMPProtocolDecoder implements
		ProtocolDecoder {

	public void decode(IoSession session, ByteBuffer in,
			ProtocolDecoderOutput out) throws ProtocolCodecException {

		final ProtocolState state = (ProtocolState) session
				.getAttribute(ProtocolState.SESSION_KEY);

		ByteBuffer buf = (ByteBuffer) session.getAttribute("buffer");
		if (buf == null) {
			buf = ByteBuffer.allocate(2048);
			buf.setAutoExpand(true);
			session.setAttribute("buffer", buf);
		}
		buf.put(in);
		buf.flip();

		List objects = decodeBuffer(state, buf);
		if (objects == null || objects.isEmpty())
			return;

		Iterator it = objects.iterator();
		while (it.hasNext())
			out.write(it.next());
	}

	public void dispose(IoSession ioSession) throws Exception {
		// TODO Auto-generated method stub

	}
}
