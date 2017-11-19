package com.lulu.openfire.plugin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.util.PropertyUtil;

public class ConfigActionServlet extends HttpServlet {
	
	private static final long serialVersionUID = -827073617548281023L;
	
	public ConfigActionServlet(){
		System.out.println("ConfigActionServlet 初始化.");
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
	
	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String config = req.getParameter("config");
		resp.setContentType("application/json;charset=UTF-8");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		if(config!=null){
			PropertyUtil.setProperty("url", config);
			// 因为所有的路径都被openfire拦截了，所以想配置自定义的页面返回需要修改
			// openfire_src/src/web/WEB-INF/decorators.xml文件，把自己的页面set进去，不让openfire过滤
			// 参考： http://blog.csdn.net/majian_1987/article/details/9927793
//			req.getSession().setAttribute("config", PropertyUtil.getProperty("url"));
//			resp.sendRedirect("success.jsp");
			out.print("设置的url地址是:"+PropertyUtil.getProperty("url"));
			out.flush();
		}else{
			out.print("设置的url地址是:"+PropertyUtil.getProperty("url"));
			out.flush();
		}
	}
}
