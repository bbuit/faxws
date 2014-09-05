package org.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.service.HylaFaxService;

public class FaxProperties {
    static public String HOST = "HOST";
    static public String USER = "USER";
    static public String PASSWD = "PASSWORD";
    static public String FAXLINES = "FAXLINES";
    static public String SERVICE = "SERVICE";
    
    static private FaxProperties faxProperties;
    private final static Log log = LogFactory.getLog(HylaFaxService.class);
    private Properties properties;
    
    synchronized static public FaxProperties getInstance() {
	if( faxProperties == null ) {
	    init();
	}
	
	return faxProperties; 	
    }
    
    static private void init() {
	faxProperties = new FaxProperties();	
	faxProperties.properties = new Properties();
	String url = "/faxProperties.properties";
	
	try {
	    InputStream is = faxProperties.getClass().getResourceAsStream(url);
	    if (is == null) is = new FileInputStream(url);
		    
	    faxProperties.properties.load(is);
	}
	catch(Exception e ) {
	    log.info("No Propery file");
	}
    }
    
    public String getProperty(String key) {
	return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultval ) {
	String ret;
	
	if( (ret = getProperty(key)) == null ) {
	    ret = defaultval;
	}
	
	return ret;
    }
   
    
    public void setProperty(String key, String property) {
	if( key != null && property != null ) {
	    properties.setProperty(key, property);
	}
    }

}
