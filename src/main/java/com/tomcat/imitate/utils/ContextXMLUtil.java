package com.tomcat.imitate.utils;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @ClassName ContextXMLUtil
 * @Description 与context.xml配置文件相关的工具类，通过该工具类获取配置文件内的信息
 * @Author Administrator
 * @Date 2022-04-01 18:36
 * @Version 1.0.0
 */
public class ContextXMLUtil {
    /**
     * 获取资源监听的文件路径
     * 在context.xml文件中进行配置
     *
     * @return
     */
    public static String getWatchedResource() {
        try {
            String xml = FileUtil.readUtf8String(Constant.contextXmlFile);
            Document document = Jsoup.parse(xml);
            Element element = document.select("WatchedResource").first();
            return element.text();
        } catch (Exception e) {
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }
}
