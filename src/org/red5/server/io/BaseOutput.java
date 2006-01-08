package org.red5.server.io;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright � 2006 by respective authors (see below). All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.util.HashMap;
import java.util.Map;

/**
 * BaseOutput represents a way to map input to a HashMap.  This class
 * is meant to be extended.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class BaseOutput {

	protected Map refMap;
	protected short refId = 0;
	
	/**
	 * BaseOutput Constructor
	 *
	 */
	protected BaseOutput(){
		refMap = new HashMap();
	}
	
	/**
	 * Store an object into a map
	 * @param obj
	 * @return void
	 */
	public void storeReference(Object obj){
		refMap.put(obj,new Short(refId++));
	}
	
	/**
	 * Returns a boolean stating whether the map contains an object by
	 * that key
	 * @param obj
	 * @return boolean
	 */
	public boolean hasReference(Object obj){
		//System.out.println("obj"+obj);
		//System.out.println("simpletest"+obj.hashCode());
		//System.out.println("has reference?"+refMap.containsKey(obj));
		return refMap.containsKey(obj);
	}
	
	/**
	 * Clears the map
	 * @return void
	 */
	public void clearReferences(){
		refMap.clear();
		refId = 0;
	}
	
	/**
	 * Returns the reference id based on the parameter obj
	 * @param obj
	 * @return short
	 */
	protected short getReferenceId(Object obj){
		return ((Short) refMap.get(obj)).shortValue();
	}

}
