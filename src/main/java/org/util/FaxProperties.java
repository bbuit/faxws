/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
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
    static public String SSL_PROTOCOL = "SSL_PROTOCOL";
    public static String READ_TIMEOUT = "READ_TIMEOUT";
    public static String CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
    
    public static enum AvailableService { HYLAFAX, SRFAX }
    
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
    
    public String getProperty( String key, String defaultval ) {
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
    
    public AvailableService getAvailableService() {  	
    	String service = getProperty(FaxProperties.SERVICE).trim();
    	return AvailableService.valueOf( service.toUpperCase() );
    }

}
