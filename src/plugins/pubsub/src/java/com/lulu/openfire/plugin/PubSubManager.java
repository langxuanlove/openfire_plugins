package com.lulu.openfire.plugin;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.PubSubModule;

public class PubSubManager {
	private PubSubModule pubSubModule = null;
	private static PubSubManager instance = null;
	
	private PubSubManager(){
		pubSubModule = XMPPServer.getInstance().getPubSubModule();
	}
	
	public static PubSubManager getInstance(){
		if(instance == null){			
			instance = new PubSubManager();
		}
		return instance;
	}
	
	public List<Node> getToptics(){
		List<Node> list = new ArrayList<Node>();
		list.addAll(pubSubModule.getNodes());
		return list;
	}
	
	public List<NodeSubscription> getTopticSubscribers(String topicId){
		List<NodeSubscription> list = new ArrayList<NodeSubscription>();
		Node node = pubSubModule.getNode(topicId);
		list.addAll(node.getSubscriptions());
		return list;
	}
	
	public boolean removeTopic(String topicId){
		Node node = pubSubModule.getNode(topicId);
		return node.delete();
	}
	
	public Node getTopic(String topicId){
		Node node = pubSubModule.getNode(topicId);
		return node;
	}
}
