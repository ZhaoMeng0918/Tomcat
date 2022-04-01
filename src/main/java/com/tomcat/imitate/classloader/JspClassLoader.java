package com.tomcat.imitate.classloader;

import com.tomcat.imitate.catalina.Context;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName JspClassLoader
 * @Description JspClassLoader特点:
 * 1. 一个jsp对应一个JspClassLoader
 * 2. 如果这个jsp文件修改了, 那么就要更换一个新的JspClassLoader
 * 3. JspClassLoader基于由jsp文件转移并编译出来的class文件进行类的加载
 * @Author Administrator
 * @Date 2022-04-01 16:41
 * @Version 1.0.0
 */
public class JspClassLoader extends URLClassLoader {
    // jsp映射JspClassLoader
    private static Map<String, JspClassLoader> map = new ConcurrentHashMap<>();

    public static void invalidJspClassLoader(String uri, Context context) {

    }

    public JspClassLoader(Context context) {
        super(new URL[0], null);
    }
}
