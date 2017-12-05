主要是针对源码，进行插件开发<br/>
1.可以发送http请求到客户端<br/>
2.插件配置，页面录入jpush的url<br/>
3.离线消息监听插件,同时发送极光推送消息<br/>
4.融合springmvc插件springcxf,发布cxf接口;<br/>
5.修改jiveClassLoader类加载spring的lib包<br/>
（Tips:springmvc启动的时候加载的是thread.getCurrent().getContextLoader()当前线程,jiveClassLoader就是当前线程方式加载的lib,puluginsClassLoader是插件的加载loader并不是当前线程的。所以想要启动springmvc容器必须更改加载springcxf插件的方式,因而修改了JiveClassLoader代码，加载了spring部分的lib包）
6.userSession监听,groupSession监听,Session监听<br/>
