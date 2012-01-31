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

package org.red5.server.api.stream;

import org.red5.server.api.IScope;

public interface IRtmpSampleAccess {
	
	public static String BEAN_NAME = "rtmpSampleAccess";

	/**
	 * Return true if sample access allowed on audio stream
	 * @param scope
	 * @return true if sample access allowed on audio stream
	 */
	public boolean isAudioAllowed(IScope scope);
	
	/**
	 * Return true if sample access allowed on video stream
	 * @param scope
	 * @return true if sample access allowed on video stream
	 */
	public boolean isVideoAllowed(IScope scope);
	
}
