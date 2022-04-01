package com.tomcat.imitate.http;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName StandardFilterConfig
 * @Description Filter配置类
 * @Author GerryZhao
 * @Date 2022-04-01 20:29
 * @Version 1.0.0
 */
public class StandardFilterConfig implements FilterConfig {
    // 应用上下文
    private ServletContext servletContext;

    // Filter名称
    private String filterName;

    // Filter初始化参数
    private Map<String, String> initParameters;

    public StandardFilterConfig(ServletContext servletContext, String filterName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.filterName = filterName;
        this.initParameters = initParameters;
        if (null == this.initParameters) {
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getFilterName() {
        return filterName;
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
