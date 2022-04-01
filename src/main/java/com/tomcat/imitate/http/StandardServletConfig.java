package com.tomcat.imitate.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName StandardServletConfig
 * @Description Servlet配置类
 * @Author Administrator
 * @Date 2022-04-01 20:07
 * @Version 1.0.0
 */
public class StandardServletConfig implements ServletConfig {
    // 应用上下文
    private ServletContext servletContext;

    // Servlet名称
    private String servletName;

    // Servlet初始化参数
    private Map<String, String> initParameters;

    public StandardServletConfig(ServletContext servletContext, String servletName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;
        if (null == this.initParameters) {
            this.initParameters = new HashMap();
        }
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }
}
