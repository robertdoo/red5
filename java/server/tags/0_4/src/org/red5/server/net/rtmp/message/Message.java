package org.red5.server.net.rtmp.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;

public class Message implements Constants {
	
	protected static Log log =
        LogFactory.getLog(Message.class.getName());
	
	private byte dataType = 0;
	private int refCount = 0;
	private ByteBuffer data;
	private boolean sealed = false;
	private int timestamp = 0;
	
	public Message(byte dataType, int initialCapacity){
		this.dataType = dataType;
		//data = ByteBuffer.allocate(initialCapacity);
		//data.setAutoExpand(true);
		//data.acquire(); // this stops it being released
		acquire();
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public byte getDataType() {
		return dataType;
	}

	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	public void acquire(){
		refCount++;
	}
	
	public void release(){
		refCount--;
		//log.info("RELEASE ? "+refCount);
		if(refCount == 0){
			if (data != null){
				//log.info("RELEASE MESSAGE : "+data.capacity());
				data.release();
			}
			doRelease();
		}
	}
	
	public boolean isSealed() {
		return sealed;
	}

	public void setSealed(boolean sealed) {
		this.sealed = sealed;
	}

	protected void doRelease(){
		//this.data.release();
	}
	
	public ByteBuffer getData() {
		return data;
	}

	public void setData(ByteBuffer data) {
	    if(this.data != null && this.data != data) {
	    	if(refCount > 0) this.data.release(); 
	    	this.refCount = 1;
	    }
		this.data = data;
	}
	
}
