package com.tomcat.imitate.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @ClassName CustomClassLoader
 * @Description 自定义类加载器, 用来加载lib文件夹下的所有jar包
 * @Author GerryZhao
 * @Date 2022-04-01 15:57
 * @Version 1.0.0
 */
public class CustomClassLoader extends URLClassLoader {
    public CustomClassLoader() {
        super(new URL[0]);
        File libFolder = new File(System.getProperty("user.dir"), "lib");
        File[] jarFiles = libFolder.listFiles();
        if (jarFiles != null) {
            for (File file : jarFiles) {
                if (file.getName().endsWith("jar")) {
                    try {
                        URL url = new URL("file:" + file.getAbsolutePath());
                        addURL(url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
