package com.lulu.openfire.plugin;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmpp.packet.JID;

public class PubSubActionServlet extends HttpServlet {
	
	private static final long serialVersionUID = -827073617548281023L;
	
	public PubSubActionServlet(){
		System.out.println("PubSubActionServlet constructor.");
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
	
	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		String action = req.getParameter("action");
		String result  = "";
		if(action ==null){
			return;
		}
		if(action.equals("removeTopic")){
			result = removeTopic(req.getParameter("topicId")) + "";
		}else if(action.equals("getSubscribers")){
			result = getSubscribers(req.getParameter("topicId"));			
		}else if(action.equals("removeSubscriber")){
			result = removeSubscriber(req.getParameter("topicId"), req.getParameter("jid")) + "";			
		}
		
		resp.getWriter().write(result);
	}

	private boolean removeTopic(String topicId){
		return PubSubManager.getInstance().removeTopic(topicId);
	}
	
	private String getSubscribers(String topicId){
		List<NodeSubscription> list = PubSubManager.getInstance().getTopticSubscribers(topicId);
		JSONArray array = new JSONArray();
		JSONObject obj = null;
		for(NodeSubscription s: list){
			try {
				obj = new JSONObject();
				obj.put("jid", s.getJID());
				array.put(obj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}		
		return array.toString();
	}
	
	private boolean removeSubscriber(String topicId, String jid){
		Node topic = PubSubManager.getInstance().getTopic(topicId);
		JID jID= new JID(jid);
		NodeSubscription nodesub = topic.getSubscription(jID);
		topic.cancelSubscription(nodesub);
		return true;
	}

}
