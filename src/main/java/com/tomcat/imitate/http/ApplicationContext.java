package com.tomcat.imitate.http;

import com.tomcat.imitate.catalina.Context;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName ApplicationContext
 * @Description 应用上下文类, Context中用来存储应用的上下文数据
 * @Author Administrator
 * @Date 2022-04-01 18:48
 * @Version 1.0.0
 */
public class ApplicationContext extends BaseServletContext {
    private Context context;

    private Map<String, Object> attributesMap = new ConcurrentHashMap<>();

    public ApplicationContext(Context context) {
        this.context = context;
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attributesMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributesMap.get(name);
    }

    @Override
    public void removeAttribute(String name) {
        this.attributesMap.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = this.attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public String getRealPath(String path) {
        return (new File(this.context.getDocBase(), path)).getAbsolutePath();
    }
}
