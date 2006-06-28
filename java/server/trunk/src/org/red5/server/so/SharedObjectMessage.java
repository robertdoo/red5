package org.red5.server.so;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.event.BaseEvent;

public class SharedObjectMessage extends BaseEvent implements ISharedObjectMessage {

	private String name;
	private LinkedList<ISharedObjectEvent> events = new LinkedList<ISharedObjectEvent>();
	private int version = 0;
	private boolean persistent = false;
	
	public SharedObjectMessage(String name, int version, boolean persistent) {
		this(null, name, version, persistent);
	}
	
	public SharedObjectMessage(IEventListener source, String name, int version, boolean persistent){
		super(Type.SHARED_OBJECT, source);
		this.name = name;
		this.version = version;
		this.persistent = persistent;
	}

	public void setSource(IEventListener source) {
		this.source = source;
	}
	
	public byte getDataType() {
		return TYPE_SHARED_OBJECT; 
	}
	
	public int getVersion() {
		return version;
	}
	
	protected void setVersion(int version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
	
	protected void setIsPersistent(boolean persistent) {
		this.persistent = persistent;
	}
	
	public void addEvent(ISharedObjectEvent event){
		events.add(event);
	}
	
	public void addEvents(List<ISharedObjectEvent> events){
		this.events.addAll(events);
	}
	
	public LinkedList<ISharedObjectEvent> getEvents(){
		return events;
	}

	public void addEvent(ISharedObjectEvent.Type type, String key, Object value) {
		events.add(new SharedObjectEvent(type, key, value));
	}

	public void clear() {
		events.clear();
	}

	public boolean isEmpty() {
		return events.isEmpty();
	}

	public Type getType() {
		return Type.SHARED_OBJECT;
	}

	public Object getObject() {
		return getEvents();
	}

	public boolean hasSource() {
		return source != null;
	}

	public IEventListener getSource() {
		return source;
	}

	public String toString(){
		final StringBuffer sb = new StringBuffer();
		sb.append("SharedObjectMessage: ").append(name).append(" { ");
		final Iterator it = events.iterator();
		while(it.hasNext()){
			sb.append(it.next());
			if(it.hasNext()) sb.append(" , ");
		}
		sb.append(" } ");
		return sb.toString();
	}
	
}
