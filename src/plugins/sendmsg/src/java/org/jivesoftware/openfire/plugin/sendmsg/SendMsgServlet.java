package org.jivesoftware.openfire.plugin.sendmsg;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.util.PropertyUtil;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

public class SendMsgServlet extends HttpServlet implements Component {
	private static final long serialVersionUID = -5404916983906926869L;

	private static final String SERVICE_NAME = "sendmsg/sendservlet";// 服务名
	private ComponentManager componentManager;// 组件管理
	private static final String COMPONENTNAME = "sendservlet";// 组件名
	private static final String domain = "@gnet-openfire";// 服务器域名
	 private static final String JPUSH_URL =
	        	"http://192.168.1.145:8080/Gnet_QM_PushService/page/pushController/pushType";
	    private static final String JPUSH_PARAMS =
	        	"\"USERIDINFO\":{0},\"CONTENT\":\"{1}\",\"PACKAGENAME\":\"{2}\","
	        	+ "\"GOTYPE\":\"{3}\"";

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);System.out.println("初始化插件。。。。。。。。。。");
		// 给验证器添加排除的路径
		AuthCheckFilter.addExclude(SERVICE_NAME);
		// 注册组件
		componentManager = ComponentManagerFactory.getComponentManager();
		try {
			componentManager.addComponent(COMPONENTNAME, this);
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	/**
	 * servlet的get方法,在这里发送信息
	 * */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String fromPerson=request.getParameter("fromPerson");
		String toPerson=request.getParameter("toPerson");
		String message=request.getParameter("message");
		try {
			// 构建信息
			Message msg = new Message();
			msg.setBody(message);
			msg.setFrom(fromPerson + domain);// 发信人
			msg.setTo(toPerson + domain);// 接收人
			msg.setType(Message.Type.chat);// 为聊天信息
			componentManager.sendPacket(this, msg);// 发送
		} catch (ComponentException err) {
			err.printStackTrace();
		}
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = new PrintWriter(response.getOutputStream());
		out.print(message+"发送成功!"+PropertyUtil.getProperty("url"));
		out.flush();
	}

	/**
	 * servlet的post方法，直接调用get方法
	 * */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	public void destroy() {
		super.destroy();
		if (componentManager != null) {
			try {
				componentManager.removeComponent(COMPONENTNAME);
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
		componentManager = null;
		AuthCheckFilter.addExclude(SERVICE_NAME);
	}

	@Override
	public String getDescription() {
		return "no Description..";
	}

	@Override
	public String getName() {
		return COMPONENTNAME;
	}

	@Override
	public void initialize(JID jid, ComponentManager comManager)
			throws ComponentException {
	}
	@Override
	public void processPacket(Packet arg0) {
	}
	@Override
	public void shutdown() {
	}
	@Override
	public void start() {
	}
}
