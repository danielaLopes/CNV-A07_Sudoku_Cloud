package pt.ulisboa.tecnico.cnv.loadbalancer.config;

import java.io.*;
import java.util.Properties;

public class AWSConfiguration {
    private static AWSConfiguration _instance = null;
    private Properties _properties = new Properties();

    public AWSConfiguration() {
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            System.out.println("dir: " + classloader.toString());
            InputStream stream = classloader.getResourceAsStream("configuration.properties");
            if(stream == null){
                throw new RuntimeException("Configuration file configuration.properties is required!");
            }
            _properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AWSConfiguration getInstance() {
        if (_instance == null) {
            _instance = new AWSConfiguration();
        }
        return _instance;
    }

    public String getRegion() {

        return _properties.getProperty("aws.region");
    }

    public String getWebServerSecurityGroup() {

        return _properties.getProperty("aws.ec2.webServer.securityGroup");
    }

    public String getWebServerTagValue() {

        return _properties.getProperty("aws.ec2.webServer.tagValue");
    }

    public String getWebServerImageId() {

        return _properties.getProperty("aws.ec2.webServer.imageId");
    }

    public String getWebServerKeyName() {

        return _properties.getProperty("aws.ec2.webServer.keyname");
    }

    public String getWebServerInstanceType() {

        return _properties.getProperty("aws.ec2.webServer.instanceType");
    }

    public long getWebServerCheckPeriod() {
        return Long.parseLong(_properties.getProperty("aws.ec2.webServer.checkperiod"));
    }

    public long getAutoScalerCheckPeriod() {
        return Long.parseLong(_properties.getProperty("autoscaler.check.period"));
    }

    public int getAutoScalerUpscaleThreshold() {
        return Integer.parseInt(_properties.getProperty("autoscaler.upscale.threshold"));
    }

    public int getAutoScalerDownscaleThreshold() {
        return Integer.parseInt(_properties.getProperty("autoscaler.downscale.threshold"));
    }
}
