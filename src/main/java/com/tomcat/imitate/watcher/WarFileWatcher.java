package com.tomcat.imitate.watcher;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import com.tomcat.imitate.catalina.Host;
import com.tomcat.imitate.utils.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

/**
 * @ClassName WarFileWatcher
 * @Description war包监视器, 监听war包的变化, 实现war包的动态热部署
 * 当前仅支持新增war包的解压和部署, 不支持war包项目的更新
 * @Author GerryZhao
 * @Date 2022-04-01 20:42
 * @Version 1.0.0
 */
public class WarFileWatcher {
    private WatchMonitor monitor;

    public WarFileWatcher(final Host host) {
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {
            private void dealWith(WatchEvent<?> event, Path currentPath) {
                synchronized (WarFileWatcher.class) {
                    String fileName = event.context().toString();
                    System.out.println(fileName);
                    if (fileName.toLowerCase().endsWith(".war") && StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
                        File warFile = FileUtil.file(Constant.webappsFolder, fileName);
                        // 注意，这里虽然调用了host的loadWar，但是并不会起到热更新的作用
                        // 因为host发现已经存在同名的应用的话，压根不会解压更新后的war包
                        // 此处war包热更新需要修改
                        host.loadWar(warFile);
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }
        });
    }

    public void start() {
        this.monitor.start();
    }

    public void stop() {
        this.monitor.interrupt();
    }
}
