package org.red5.server;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
import org.red5.server.api.IScope;
import org.red5.server.api.IServer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xml.sax.SAXException;

public class JettyLoader implements ApplicationContextAware {
	
	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(JettyLoader.class.getName());
	
	protected String jettyConfig = "classpath:/jetty.xml";
	protected Server jetty;
	// We store the application context in a ThreadLocal so we can access it from
	// "org.red5.server.jetty.Red5WebPropertiesConfiguration" later.
	private static ThreadLocal<ApplicationContext> applicationContext = new ThreadLocal<ApplicationContext>();
	
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		applicationContext.set(context);
	}
	
	public static ApplicationContext getApplicationContext() {
		return applicationContext.get();
	}
	
	public void init() {
		// Originally this class was used to inspect the webapps.
		// But now thats done using Red5WebPropertiesConfiguration
		// So this class is left just starting jetty, we can probably use the old method 
		try {
			log.info("Loading jetty6 context from: "+jettyConfig);
			Server jetty = new Server();
			XmlConfiguration config = new XmlConfiguration(getApplicationContext().getResource(jettyConfig).getInputStream());
			config.configure(jetty);
			log.info("Starting jetty servlet engine");
			jetty.start();
		} catch (Exception e) {
			log.error("Error loading jetty", e);
		}
		
	}
	
}
