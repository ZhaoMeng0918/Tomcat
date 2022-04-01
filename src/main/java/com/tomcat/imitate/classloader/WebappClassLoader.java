package com.tomcat.imitate.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * @ClassName WebappClassLoader
 * @Description 用来加载用户编写的java类，一个应用对应一个WebappClassLoader
 * @Author GerryZhao
 * @Date 2022-04-01 16:13
 * @Version 1.0.0
 */
public class WebappClassLoader extends URLClassLoader {
    public WebappClassLoader(String docBase, ClassLoader CustomClassLoader) {
        super(new URL[0], CustomClassLoader);
        try {
            File webInfFolder = new File(docBase, "WEB-INF");
            File classesFolder = new File(webInfFolder, "classes");
            File libFolder = new File(webInfFolder, "lib");
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            addURL(url);
            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for (File file: jarFiles) {
                url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
                addURL(url);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
