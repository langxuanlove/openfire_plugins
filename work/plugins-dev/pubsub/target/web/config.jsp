<!DOCTYPE HTML>
<html>
<head>
<title>jpush config</title>

<meta name="pageID" content="nodelist" />
<style type="text/css">
	.clear{
		clear:both;
	}
	#topic-list{
		width: 100%
	}
	.loginForm input[type=text], .loginForm input[type=password] {
	margin-bottom: 10px;
}
.loginForm input[type=submit] {
	background: #fb044a;
	color: #fff;
	font-weight: 200;
}
</style>
</head>
<body>
<div id="topic-list">
		<form action="/plugins/pubsub/config/properties" class="loginForm">
				<div class="input-group">
					<input type="text" id="config" name="config" class="form-control"
						placeholder="jpush url">
					 <input type="submit" id="submit" class="form-control" value="confirm">
				</div>
		</form>
</div>
</body>
</html>
