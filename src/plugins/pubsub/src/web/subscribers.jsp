<%@page import="org.jivesoftware.openfire.pubsub.NodeSubscription"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.jivesoftware.openfire.pubsub.Node"%>
<%@page import="com.lulu.openfire.plugin.PubSubManager" %>

<!DOCTYPE HTML>
<html>
<head>
<title>My Plugin Page</title>

<style type="text/css">
	.clear{
		clear:both;
	}
	#subscribe-list{
		width: 100%
	}
</style>
<script type="text/javascript" src="js/jquery-1.8.2.js"></script>
<script type="text/javascript">
	$(document).ready(function(){
	});
	
</script>
</head>
<body>
<div>Subscribers list</div>
<div id="subscribe-list">
	<table border="1">
		<thead>
			<tr><td>Subscriber</td><td>Action</td></tr>
		</thead>
		<tbody>
	
<%
	PubSubManager m = PubSubManager.getInstance();
	String topicId = request.getParameter("topicId");
	for(NodeSubscription s:m.getTopticSubscribers(topicId)){ 
%>
	<tr>
		<td><%= s.getJID()%></td>
		<td><input type="button" value="remove" /></td>
	</tr>
<%
	}
%>
	</tbody>
	</table>
</div>
</body>
</html>
