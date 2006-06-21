package org.red5.server.net.rtmp.codec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.Input;
import org.red5.io.object.Deserializer;
import org.red5.io.utils.BufferUtils;
import org.red5.server.DebugPooledByteBufferAllocator;
import org.red5.server.net.protocol.ProtocolException;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.ChunkSize;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.InPacket;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.PacketHeader;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.SharedObject;
import org.red5.server.net.rtmp.message.SharedObjectEvent;
import org.red5.server.net.rtmp.message.StreamBytesRead;
import org.red5.server.net.rtmp.message.Unknown;
import org.red5.server.net.rtmp.message.VideoData;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;

public class RTMPProtocolDecoder implements Constants, SimpleProtocolDecoder {

	protected static Log log =
        LogFactory.getLog(RTMPProtocolDecoder.class.getName());

	protected static Log ioLog =
        LogFactory.getLog(RTMPProtocolDecoder.class.getName()+".in");
	
	private Deserializer deserializer = null;

	public RTMPProtocolDecoder(){
		
	}
	
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}
	
    public List decodeBuffer(ProtocolState state, ByteBuffer buffer) {
		
    	List result = new LinkedList();
    	
		try {
			while(true){
			 	
				if(state.canStartDecoding(buffer.remaining())){
					//log.debug("Starting decoding");
					state.startDecoding();
				}
			    else {
			    	//log.debug("Further buffering needed: "+buf.remaining());
			    	break;
			    }
			   
				final int oldPos = buffer.position();
			    final Object decodedObject = decode( state, buffer );
			    //DebugPooledByteBufferAllocator.setCodeSection(null);
			    if(state.hasDecodedObject()){
			    	result.add(decodedObject);
			    	
			    	if( buffer.position() == oldPos ){
			            throw new IllegalStateException(
			                    "doDecode() can't return true when buffer is not consumed." );
			        }
			        if( !buffer.hasRemaining() ) {
			        	//log.debug("End of decode");
			        	break;
			        }
			    }
			    else if( state.canContinueDecoding() ) 	{
			    	//log.debug("Continue decoding");
			    	continue; 
			    }
			    else {
			    	//log.debug("Buffering: "+state.getDecoderBufferAmount());
			    	break;
			    }
			}
		}
		catch(ProtocolException  pvx){
			log.error("Error",pvx);
		}
		catch(Exception ex){
			log.error("Error",ex);
		}
		finally {
			// is this needed?
			buffer.compact();
		}
		
		return result;
	}

	public Object decode(ProtocolState state, ByteBuffer in) throws ProtocolException {
		
		//DebugPooledByteBufferAllocator.setCodeSection("decode");
		
		try {
		
			final RTMP rtmp = (RTMP) state;

			if(in.remaining() < 1){
				state.bufferDecoding(1);
				return null;
			}
			
			final int startPosition = in.position();
			//log.info("Start: "+startPosition+" size:"+in.capacity());
			
			if(rtmp.getMode()==RTMP.MODE_SERVER){
			
				if(rtmp.getState()==RTMP.STATE_CONNECT){
					
					if(in.remaining() < HANDSHAKE_SIZE + 1){ 
						log.debug("Handshake init too small, buffering. remaining: "+in.remaining());
						state.bufferDecoding(HANDSHAKE_SIZE + 1); 
						return null;
					}
					else {
					
						//	Data in the handshake, Looks like a little bit of info
						// in.get(); // id byte, always the same
						// in.getInt(); // uptime ? seems to count up
						// in.position(in.position()-5); // reset position
						
						ByteBuffer hs = ByteBuffer.allocate(HANDSHAKE_SIZE);
						hs.setAutoExpand(true);
						in.get(); // skip the header byte
						int limit = in.limit();
						in.limit(in.position() + HANDSHAKE_SIZE);
						hs.put(in).flip();
						rtmp.setState(RTMP.STATE_HANDSHAKE);
						in.limit(limit);
						return hs;
					}
				} 
				
				if(rtmp.getState()==RTMP.STATE_HANDSHAKE){
					log.debug("Handshake reply");
					if(in.remaining() < HANDSHAKE_SIZE){ 
						log.debug("Handshake reply too small, buffering. remaining: "+in.remaining());
						state.bufferDecoding(HANDSHAKE_SIZE);
						return null;
					} else {
						in.skip(HANDSHAKE_SIZE);
						rtmp.setState(RTMP.STATE_CONNECTED);
						state.continueDecoding();
						return null;
					}
				}
				
			} else {
				
				// this is client mode. 
				if(rtmp.getState()==RTMP.STATE_CONNECT){
					if(in.remaining() < (2*HANDSHAKE_SIZE)+1){ 
						log.debug("Handshake init too small, buffering. remaining: "+in.remaining());
						state.bufferDecoding((2*HANDSHAKE_SIZE)+1);
						return null;
					} else {
						ByteBuffer hs = ByteBuffer.allocate(HANDSHAKE_SIZE);
						hs.setAutoExpand(true);
						hs.put(in).flip();
						rtmp.setState(RTMP.STATE_CONNECTED);
						return hs;
					}
				} 
			}
			
			final byte headerByte = in.get();
			final byte channelId = RTMPUtils.decodeChannelId(headerByte);
			
			if(channelId<0)
				throw new ProtocolException("Bad channel id");
			
			// Get the header size and length
			final byte headerSize = (byte) RTMPUtils.decodeHeaderSize(headerByte);
			int headerLength = RTMPUtils.getHeaderLength(headerSize);
			
			if(headerLength > in.remaining()) {
				log.debug("Header too small, buffering. remaining: "+in.remaining());
				in.position(startPosition);
				state.bufferDecoding(headerLength);
				return null;
			}
			
			PacketHeader header = null;
			in.position(in.position()-1);
			
			header = decodeHeader(in,rtmp.getLastReadHeader(channelId));
			
			if(header==null){
				log.warn("Header is null");
				throw new ProtocolException("Header is null, check for error");
			}
			
			if(header!=null) rtmp.setLastReadHeader(channelId, header);
			
			InPacket packet = rtmp.getLastReadPacket(channelId);
			
			if(packet==null){
				packet = newPacket(header);
				rtmp.setLastReadPacket(channelId, packet);
			}
			
			if(packet.getMessage().getData() == null) 
				packet.getMessage().setData(ByteBuffer.allocate(header.getSize()));
			ByteBuffer buf = packet.getMessage().getData();
			int readRemaining = header.getSize() - buf.position();
			
			int chunkSize = rtmp.getReadChunkSize();
			
			final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
			
			if(in.remaining() < readAmount) {
				if(log.isDebugEnabled())
					log.debug("Chunk too small, buffering ("+in.remaining()+","+readAmount);
				in.position(startPosition);
				state.bufferDecoding(headerSize + readAmount);
				return null;
			}
			
			//log.debug("in: "+in.remaining()+" read: "+readAmount+" pos: "+buf.position());
			
			try {
				BufferUtils.put(buf, in, readAmount);
			} catch (RuntimeException e) {
				log.error("Error",e);
				throw new ProtocolException("Error copying buffer");
			}
			
			if(buf.position() >= header.getSize()){
				if(log.isDebugEnabled())
					log.debug("Finished read, decode packet");
				buf.flip();
				final Message message = packet.getMessage();
				decodeMessage(message, packet.getSource().getStreamId());
				message.setTimestamp(packet.getSource().getTimer());

				if(ioLog.isDebugEnabled()){
					ioLog.debug(packet.getSource());
					ioLog.debug(packet.getMessage());
					ioLog.debug(buf.getHexDump());
				}
				
				if(message instanceof ChunkSize){
					ChunkSize chunkSizeMsg = (ChunkSize) message;
					rtmp.setReadChunkSize(chunkSizeMsg.getSize());
				}
				rtmp.setLastReadPacket(channelId, null);
				if(!packet.getMessage().isSealed() && packet.getMessage().getData() != null){
					packet.getMessage().getData().release();
				}
				return packet;
			} else { 
				state.continueDecoding();
				return null;
			}
		} catch (RuntimeException e){
			log.error("Error", e);
			throw new ProtocolException("Error copying buffer");
		}
		
	}
	
	public PacketHeader decodeHeader(ByteBuffer in, PacketHeader lastHeader){
		
		final byte headerByte = in.get();
		final byte channelId = RTMPUtils.decodeChannelId(headerByte);
		
		final byte headerSize = (byte) RTMPUtils.decodeHeaderSize(headerByte);

		/*
		int limit  = in.limit();
		int position = in.position();
		in.limit(in.position()+11);
		log.debug("Raw Header: "+in.getHexDump());
		in.limit(limit);
		in.position(position);
		*/
		
		PacketHeader header = new PacketHeader();
		header.setChannelId(channelId);
		
		switch(headerSize){
		
		case HEADER_NEW:
			header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
			header.setSize(RTMPUtils.readMediumInt(in));
			header.setDataType(in.get());
			header.setStreamId(RTMPUtils.readReverseInt(in));
			break;
			
		case HEADER_SAME_SOURCE:			
			header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
			header.setSize(RTMPUtils.readMediumInt(in));
			header.setDataType(in.get());
			header.setStreamId(lastHeader.getStreamId());
			break;
			
		case HEADER_TIMER_CHANGE:
			header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
			header.setDataType(lastHeader.getDataType());
			header.setStreamId(lastHeader.getStreamId());
			header.setSize(lastHeader.getSize());
			break;
			
		case HEADER_CONTINUE:
			header = lastHeader;
			break;
			
		default:
			log.error("Unexpected header size: "+headerSize);
			return null;
		
		}
		return header;
	}
	
	public InPacket newPacket(PacketHeader header){
		final InPacket packet = new InPacket();
		packet.setSource(header);
		switch(header.getDataType()){
		case TYPE_CHUNK_SIZE:
			packet.setMessage(new ChunkSize());
			break;
		case TYPE_INVOKE:
			packet.setMessage(new Invoke());
			break;
		case TYPE_NOTIFY:
			packet.setMessage(new Notify());
			break;
		case TYPE_PING:
			packet.setMessage(new Ping());
			break;
		case TYPE_STREAM_BYTES_READ:
			packet.setMessage(new StreamBytesRead());
			break;
		case TYPE_AUDIO_DATA:
			packet.setMessage(new AudioData());
			break;
		case TYPE_VIDEO_DATA:
			packet.setMessage(new VideoData());
			break;
		case TYPE_SHARED_OBJECT:
			packet.setMessage(new SharedObject());
			break;
		default:
			packet.setMessage(new Unknown(header.getDataType()));
			break;
		}
		return packet;
	}
	
	public void decodeMessage(Message message, int streamId) {
		switch(message.getDataType()){
		case TYPE_CHUNK_SIZE:
			decodeChunkSize((ChunkSize) message);
			break;
		case TYPE_INVOKE:
			decodeInvoke((Invoke) message);
			break;
		case TYPE_NOTIFY:
			// This could also contain stream metadata
			if (streamId != 0)
				decodeStreamMetadata(message);
			else
				decodeNotify((Notify) message);
			break;
		case TYPE_PING:
			decodePing((Ping) message);
			break;
		case TYPE_STREAM_BYTES_READ:
			decodeStreamBytesRead((StreamBytesRead) message);
			break;
		case TYPE_AUDIO_DATA:
			decodeAudioData((AudioData) message);
			break;
		case TYPE_VIDEO_DATA:
			decodeVideoData((VideoData) message);
			break;
		case TYPE_SHARED_OBJECT:
			decodeSharedObject((SharedObject) message);
			break;
		}
	}
	
	private void decodeChunkSize(ChunkSize chunkSize) {
		chunkSize.setSize(chunkSize.getData().getInt());
	}

	public void decodeSharedObject(SharedObject so) {
		
		if(log.isDebugEnabled())
			log.debug("> "+so.getData().getHexDump());
		
		final ByteBuffer data = so.getData();

		Input input = new Input(data);
		so.setName(input.getString(data));
		// Read version of SO to modify
		so.setSoId(data.getInt());
		// Read persistence informations
		so.setType(data.getInt());
		// Skip unknown bytes
		data.skip(4);
		// Parse request body
		while(data.hasRemaining()){
			byte type = data.get();
			if(log.isDebugEnabled()) 
				log.debug("type: "+type);
			SharedObjectEvent event = new SharedObjectEvent(type,null,null);
			int length = data.getInt();
			if (type != SO_SEND_MESSAGE) {
				if (length > 0){
					event.setKey(input.getString(data));
					
					if (length > event.getKey().length()+2){
						event.setValue(deserializer.deserialize(input));
					}
				}
			} else {
				int start = data.position();
				// the "send" event seems to encode the handler name
				// as complete AMF string including the string type byte
				event.setKey((String) deserializer.deserialize(input));

				// read parameters
				LinkedList value = new LinkedList();
				while (data.position() - start < length) {
					Object tmp = deserializer.deserialize(input);
					value.add(tmp);
				}
				event.setValue(value);
			}
			so.addEvent(event);
		}
		log.debug(so);
	}

	public void decodeInvoke(Invoke invoke){
		decodeNotify((Notify) invoke);
	}
	
	public void decodeNotify(Notify notify){
		Input input = new Input(notify.getData());
		
		String action = (String) deserializer.deserialize(input);
		
		if(log.isDebugEnabled())
			log.debug("Action "+action);
		
		int invokeId = ((Number) deserializer.deserialize(input)).intValue();
		notify.setInvokeId(invokeId);
				
		Object[] params = new Object[]{};

		if(notify.getData().hasRemaining()){
			ArrayList paramList = new ArrayList();
			
			// Before the actual parameters we sometimes (connect) get a map
			// of parameters, this is usually null, but if set should be passed
			// to the connection object. 
			final Map connParams = (Map) deserializer.deserialize(input);
			notify.setConnectionParams(connParams);
			
			while(notify.getData().hasRemaining()){
				paramList.add(deserializer.deserialize(input));
			}
			params = paramList.toArray();
			if(log.isDebugEnabled()){
				log.debug("Num params: "+paramList.size()); 
				for(int i=0; i<params.length; i++){
					log.debug(" > "+i+": "+params[i]);
				}
			}
		} 
		
		// The method name has the form "<serviceName>.<serviceMethod>"
		// where "<serviceName>" may contain dots.
		final int dotIndex = action.lastIndexOf(".");
		String serviceName = (dotIndex==-1) ? null : action.substring(0,dotIndex);
		String serviceMethod = (dotIndex==-1) ? action : action.substring(dotIndex+1, action.length());
		
		if (notify instanceof Invoke) {
			PendingCall call = new PendingCall(serviceName,serviceMethod,params);
			((Invoke) notify).setCall(call);
		} else {
			Call call = new Call(serviceName,serviceMethod,params);
			notify.setCall(call);
		}
	}
	
	public void decodePing(Ping ping){
		final ByteBuffer in = ping.getData();
		ping.setValue1(in.getShort());
		ping.setValue2(in.getInt());
		if(in.remaining() > 0) ping.setValue3(in.getInt());
	}
	
	public void decodeStreamBytesRead(StreamBytesRead streamBytesRead){
		final ByteBuffer in = streamBytesRead.getData();
		streamBytesRead.setBytesRead(in.getInt());
	}
	
	public void decodeAudioData(AudioData audioData){
		//audioData.acquire();
		audioData.setSealed(true);
	}
	
	public void decodeVideoData(VideoData videoData){
		//videoData.acquire();
		videoData.setSealed(true);
	}
	
	public void decodeStreamMetadata(Message metadata){
		//metadata.acquire();
		metadata.setSealed(true);
	}
}
