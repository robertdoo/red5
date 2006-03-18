package org.red5.server.api.impl;

import org.red5.server.api.IScope;
import org.red5.server.api.IScopeResolver;
import org.red5.server.api.ScopeUtils;

public class ScopeResolver implements IScopeResolver {

	public static final String DEFAULT_HOST = "default";
	
	public IScope root;

	public IScope getRoot() {
		return root;
	}

	public void setRoot(IScope root) {
		this.root = root;
	}

	public IScope resolveScope(String host, String path){
		IScope scope = root;
		if(ScopeUtils.isGlobal(scope)){
			if(host==null) return scope;
			else if(scope.hasChildScope(host))
				scope = scope.getChildScope(host);
			else if(scope.hasChildScope(DEFAULT_HOST))
				scope = scope.getChildScope(DEFAULT_HOST);
			else throw new ScopeNotFoundException(scope,host);
		} 
		if(path == null) return scope;
		final String[] parts = path.split("/");
		if(parts.length > 0 && ScopeUtils.isHost(scope)){
			final String app = parts[0];
			if(scope.hasChildScope(app))
				scope = scope.getChildScope(app);
			else throw new ScopeNotFoundException(scope,app);
		}
		for(int i=1; i < parts.length; i++){
			final String room = parts[i];
			if(scope.hasChildScope(room)){
				scope = scope.getChildScope(room);
			} else if(scope.createChildScope(room)){
				scope = scope.getChildScope(room);
			} else throw new ScopeNotFoundException(scope,parts[i]);
		}
		return scope;
	}
	
}
