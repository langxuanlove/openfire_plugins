package org.jivesoftware.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 加载，写入properties配置文件信息
 * 
 * 
 */
public class PropertyUtil {
	// openfire源码下，jpush.properties文件在上下文openfire_src/bin目录下是可用的
	// 在生成部署文件的时候必须是在openfire_src/target/openfire/lib目录下.才是可用的。
	// 因此使用ant的拷贝功能,将openfire_src/bin 的文件拷贝到openfire_src/target/openfire/lib即可。
	// 此工具类必须在openfire的和核心类中，因为插件是后运行的。不然会报错提示找不到此工具类
	private static  String fileName="jpush.properties";
	private static  File f = new File(PropertyUtil.class.getResource("/").getPath()+"/"+fileName);
	public static Logger logger = LoggerFactory.getLogger(PropertyUtil.class);

	public static String getProperty(String key) { 
		System.out.println("文件路径是："+f);
        String value = "";  
        // 第一步是取得一个Properties对象  
        Properties props = new PropertiesImpl();  
        // 第二步是取得配置文件的输入流  
        InputStream is = PropertyUtil.class.getClassLoader().getResourceAsStream(fileName);//在非WEB环境下用这种方式比较方便  
        try {  
        	// 第三步讲配置文件的输入流load到Properties对象中，这样在后面就可以直接取来用了  
            props.load(is);  
            value = props.getProperty(key);  
            is.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        return value;  
    } 
	public static void setProperty(String key,String value) {  
		// 第一步也是取得一个Properties对象  
		Properties props = new PropertiesImpl();  
		// 第二步也是取得该配置文件的输入流  
		        try {  
		        	InputStream is = new FileInputStream(f); 
		// 第三步是把配置文件的输入流load到Properties对象中，  
		            props.load(is);  
		            is.close();  
		// 接下来就可以随便往配置文件里面添加内容了  
		            props.setProperty(key, value); 
		// 在保存配置文件之前还需要取得该配置文件的输出流，切记，如果该项目是需要导出的且是一个非WEB项目，则该配置文件应当放在根目录下，否则会提示找不到配置文件  
		            OutputStream out = new FileOutputStream(f);  
		// 最后就是利用Properties对象保存配置文件的输出流到文件中;  
		            props.store(out, "Copyright Thcic");  
		            out.close();  
		        } catch (IOException e) {  
		            e.printStackTrace();  
		        }  
		    }  
	private PropertyUtil() {}
	public static void main(String[] args) {
		String names=PropertyUtil.getProperty("custom.datasource.names");
		System.out.println(names);
		PropertyUtil.setProperty("custom.datasource.names",names+",ibd104");
		names=PropertyUtil.getProperty("custom.datasource.names");
		System.out.println(names);
		
	}
}