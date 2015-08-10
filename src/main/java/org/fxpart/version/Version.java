package org.fxpart.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by metairie on 08-Aug-15.
 * Version loader
 */
public class Version {
    private static Version instance = null;
    public static String version = "";
    public static String build = "";

    public static Version getInstance() {
        if (instance == null) {
            synchronized (Version.class) {
                if (instance == null) {
                    instance = new Version();
                }
            }
        }
        return instance;
    }

    protected Version() {
        Properties properties = new Properties();
        InputStream input = null;
        try {

            input = Version.class.getClassLoader().getResourceAsStream("org/fxpart/version/version.properties");
            if (input == null) {
                System.out.println("Sorry, unable to find version.properties");
                return;
            }
            properties.load(input);
            version = properties.getProperty("version");
            build = properties.getProperty("build.date");
            System.out.println("AutosuggestFX version : " + version);
            System.out.println("AutosuggestFX build : " + build);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}