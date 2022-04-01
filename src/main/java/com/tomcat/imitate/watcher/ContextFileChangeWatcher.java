package com.tomcat.imitate.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import com.tomcat.imitate.catalina.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * @ClassName ContextFileChangeWatcher
 * @Description 应用文件夹的热部署, 监听解压后的应用, 在context类中使用, 监听context文件变化, 然后重新加载context
 * @Author GerryZhao
 * @Date 2022-04-01 16:55
 * @Version 1.0.0
 */
public class ContextFileChangeWatcher {
    // 监视器, 监视文件夹是否发生变化
    private WatchMonitor monitor;

    // 防止在监听到文件变化到重新发布完成之间重复发布，一个监视器只负责一次重新部署
    private boolean stop = false;

    public ContextFileChangeWatcher(final Context context) {
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            private void dealWith(WatchEvent<?> watchEvent) {
                synchronized (ContextFileChangeWatcher.class) {
                    String fileName = watchEvent.context().toString();
                    if (!stop) {
                        if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
                            stop = true;
                            LogFactory.get().info(this + " Important file changes under the Web application were detected {}, ", fileName);
                            context.reload();
                        }
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                this.dealWith(watchEvent);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                this.dealWith(watchEvent);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                this.dealWith(watchEvent);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                this.dealWith(watchEvent);
            }
        });
        this.monitor.setDaemon(true);
    }

    public void start() {
        this.monitor.start();
    }

    public void stop() {
        this.monitor.close();
    }
}
