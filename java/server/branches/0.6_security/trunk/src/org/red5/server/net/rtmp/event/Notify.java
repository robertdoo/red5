package org.red5.server.net.rtmp.event;

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

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.stream.IStreamData;

public class Notify extends BaseEvent implements IStreamData {

	protected IServiceCall call = null;

	protected ByteBuffer data = null;

	private int invokeId = 0;

	private Map connectionParams = null;

	public Notify() {
		super(Type.SERVICE_CALL);
	}

	public Notify(ByteBuffer data) {
		super(Type.STREAM_DATA);
		this.data = data;
	}

	public Notify(IServiceCall call) {
		super(Type.SERVICE_CALL);
		this.call = call;
	}

	@Override
	public byte getDataType() {
		return TYPE_NOTIFY;
	}

	public void setData(ByteBuffer data) {
		this.data = data;
	}

	public void setCall(IServiceCall call) {
		this.call = call;
	}

	public IServiceCall getCall() {
		return this.call;
	}

	public ByteBuffer getData() {
		return data;
	}

	public int getInvokeId() {
		return invokeId;
	}

	public void setInvokeId(int invokeId) {
		this.invokeId = invokeId;
	}

	protected void doRelease() {
		call = null;
	}

	public Map getConnectionParams() {
		return connectionParams;
	}

	public void setConnectionParams(Map connectionParams) {
		this.connectionParams = connectionParams;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Notify: ").append(call);
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Notify)) {
			return false;
		}
		Notify other = (Notify) obj;
		if (getConnectionParams() == null
				&& other.getConnectionParams() != null) {
			return false;
		}
		if (getConnectionParams() != null
				&& other.getConnectionParams() == null) {
			return false;
		}
		if (getConnectionParams() != null
				&& !getConnectionParams().equals(other.getConnectionParams())) {
			return false;
		}
		if (getInvokeId() != other.getInvokeId()) {
			return false;
		}
		if (getCall() == null && other.getCall() != null) {
			return false;
		}
		if (getCall() != null && other.getCall() == null) {
			return false;
		}
		if (getCall() != null && !getCall().equals(other.getCall())) {
			return false;
		}
		return true;
	}

	@Override
	protected void releaseInternal() {
		if (data != null) {
			data.release();
			data = null;
		}
	}

}