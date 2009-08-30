package org.red5.server.plugin;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.plugin.IRed5Plugin;
import org.slf4j.Logger;

/**
 * Central registry for Red5 plug-ins.
 * 
 * @author Paul Gregoire
 */
public class PluginRegistry {

	private static Logger log = Red5LoggerFactory.getLogger(PluginRegistry.class, "plugins");

	//keeps track of plug-ins, keyed by plug-in name
	private static volatile ConcurrentMap<String, IRed5Plugin> plugins = new ConcurrentHashMap<String, IRed5Plugin>();
	
	//locks for guarding plug-ins
	private final static ReadWriteLock pluginLock = new ReentrantReadWriteLock(); 
    private final static Lock pluginReadLock; 
    private final static Lock pluginWriteLock; 	
	
	static {
		pluginReadLock = pluginLock.readLock(); 
		pluginWriteLock = pluginLock.writeLock(); 
	}
    
	/**
	 * Registers a plug-in.
	 * 
	 * @param plugin
	 */
	public static void register(IRed5Plugin plugin) {
		log.debug("Register plugin: {}", plugin);
		String pluginName = plugin.getName();
		//get a write lock
		pluginWriteLock.lock();
		try {
			if (plugins.containsKey(pluginName)) {
				//get old plugin
				IRed5Plugin oldPlugin = plugins.get(pluginName);
				//if they are not the same shutdown the older one
				if (!plugin.equals(oldPlugin)) {			
					oldPlugin.doStop();
					//replace old one
					plugins.replace(pluginName, plugin);
				}
			} else {
				plugins.put(pluginName, plugin);
			}
		} finally {
			pluginWriteLock.unlock();
		}		
	}
	
	/**
	 * Unregisters a plug-in.
	 * 
	 * @param plugin
	 */
	public static void unregister(IRed5Plugin plugin) {
		log.debug("Unregister plugin: {}", plugin);
		//get a write lock
		pluginWriteLock.lock();
		try {
			if (plugins.containsValue(plugin)) {
				boolean removed = false;
				for (Entry<String, IRed5Plugin> f : plugins.entrySet()) {
					if (plugin.equals(f.getValue())) {
						log.debug("Removing {}", plugin);
						plugins.remove(f.getKey());
						removed = true;
						break;
					} else {
						log.debug("Not equal - {} {}", plugin, f.getValue());
					}
				}
				if (!removed) {
					log.debug("Last try to remove the plugin");
					plugins.remove(plugin.getName());
				}
			} else {
				log.warn("Plugin is not registered {}", plugin);
			}
		} finally {
			pluginWriteLock.unlock();
		}			
	}
	
	/**
	 * Returns a plug-in.
	 * 
	 * @param pluginName
	 * @return requested plug-in matching the name given or null if not found
	 */
	public static IRed5Plugin getPlugin(String pluginName) {
		IRed5Plugin plugin = null;
		pluginReadLock.lock();
		try {
			plugin = plugins.get(pluginName);
		} finally {
			pluginReadLock.unlock();
		}
		return plugin;
	}	
	
	/**
	 * Shuts down the registry and stops any plug-ins that are found.
	 * 
	 * @throws Exception
	 */
	public static void shutdown() throws Exception {
		log.info("Destroying and cleaning up {} plugins", plugins.size());	
		//loop through the plugins and stop them
		pluginReadLock.lock();
		try {
			for (Entry<String, IRed5Plugin> plugin : plugins.entrySet()) {
				plugin.getValue().doStop();
			}
		} finally {
			pluginReadLock.unlock();
		}
		plugins.clear();
	}
	
}
