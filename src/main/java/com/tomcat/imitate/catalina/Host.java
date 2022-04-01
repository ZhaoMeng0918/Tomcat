package com.tomcat.imitate.catalina;

import com.tomcat.imitate.watcher.WarFileWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName Host
 * @Description 一个Host包含多个应用
 * @Author Administrator
 * @Date 2022-04-01 16:47
 * @Version 1.0.0
 */
public class Host {
    private String name;

    private Engine engine;

    private Map<String, Context> contextMap;

    public Host(String name, Engine engine) {
        this.name = name;
        this.engine = engine;
        this.contextMap = new HashMap<>();

        // 扫描并添加webapps文件夹下的应用
        scanContextsOnWebAppsFolder();

        // 扫描并加载server.xml中配置的应用
        scanContextsInServerXML();

        // 扫描并加载webapps文件夹下的war包, 解压并加载
        scanWarOnWebAppsFolder();

        // 监听war包, 实现动态部署
        WarFileWatcher warFileWatcher = new WarFileWatcher(this);
        warFileWatcher.start();
    }

    /**
     * 扫描webapps目录下的应用
     */
    private void scanContextsOnWebAppsFolder() {

    }

    /**
     * 根据server.xml加载应用
     */
    private void scanContextsInServerXML() {

    }

    /**
     * 加载上下文应用
     *
     * @param folder
     */
    private void loadContext(File folder) {

    }

    /**
     * 重新加载context
     *
     * @param context
     */
    public void reload(Context context) {

    }

    /**
     * 扫描webapps目录下的所有war文件
     */
    private void scanWarOnWebAppsFolder() {

    }

    /**
     * 把
     *
     * @param warFile
     */
    public void loadWar(File warFile) {

    }
}
