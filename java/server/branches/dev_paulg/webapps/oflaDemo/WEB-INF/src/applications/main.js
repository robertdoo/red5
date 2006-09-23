/*
 * main.js - a translation into JavaScript of the olfa demo Application class, a red5 example.
 *
 * @author Paul Gregoire
 */

importPackage(Packages.org.red5.server.api);
importPackage(Packages.org.red5.server.api.stream);
importPackage(Packages.org.red5.server.api.stream.support);
importPackage(Packages.org.apache.commons.logging);

importClass(Packages.org.springframework.core.io.Resource);
importClass(Packages.org.red5.server.api.Red5);
importClass(Packages.org.red5.server.api.IScopeHandler);

var IStreamCapableConnection = Packages.org.red5.server.api.stream.IStreamCapableConnection;

function Application() {
	var appScope = null;
	var serverStream = null;

    print('Application\n');
    /*
    for (property in this.__proto__) {
		try {
			print('>>>' + property);
		} catch(e) {
			e.rhinoException.printStackTrace();
		}	
	}
 
    print('\nApplicationAdapter\n');
	for (property in this.__proto__.__proto__) {
		try {
			print('>>>' + property);
		} catch(e) {
			e.rhinoException.printStackTrace();
		}	
	}	
    */
}	

Application.prototype.appStart = function(app) {
	print('Javascript appStart');
	this.appScope = app;
	return true;
};

Application.prototype.appConnect = function(conn, params) {
	print('Javascript appConnect');
	measureBandwidth(conn);
	if (conn == typeof(IStreamCapableConnection)) {
		var streamConn = conn;
		var sbc = new Packages.org.red5.server.api.stream.support.SimpleBandwidthConfigure();
		sbc.setMaxBurst(8388608);
		sbc.setBurst(8388608);
		sbc.setOverallBandwidth(2097152);
		streamConn.setBandwidthConfigure(sbc);
	}
	return appConnect(conn, params);
};

Application.prototype.appDisconnect = function(conn) {
	print('Javascript appDisconnect');
	if (this.appScope == conn.getScope() && this.serverStream)  {
		this.serverStream.close();
	}
	return appDisconnect(conn);
};

Application.prototype = supa;

new Application();




