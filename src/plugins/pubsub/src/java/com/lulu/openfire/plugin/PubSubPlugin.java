package com.lulu.openfire.plugin;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubPlugin implements Plugin {
	private static Logger logger = LoggerFactory.getLogger(PubSubPlugin.class);
	@Override
	public void destroyPlugin() {
		logger.info("destroy pubsub Plugin");
	}

	@Override
	public void initializePlugin(PluginManager arg0, File arg1) {
		logger.info("initialize pubsub Plugin");
	}

}
