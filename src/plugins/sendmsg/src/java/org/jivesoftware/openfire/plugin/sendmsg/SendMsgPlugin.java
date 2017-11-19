package org.jivesoftware.openfire.plugin.sendmsg;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Packet;



public class SendMsgPlugin implements Plugin, PacketInterceptor {
	public SendMsgPlugin(){
		System.out.println("SendMsgPlugin----------------");
	}
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub
		System.out.println("SendMsgPlugin插件初始化");
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		System.out.println("SendMsgPlugin插件销毁");
		
	}
	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {
		// TODO Auto-generated method stub
		System.out.println("this is is  s ");
		
	}

}
