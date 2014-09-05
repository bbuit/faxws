package org.service;

import org.util.FaxProperties;

public class FaxServiceFactory {
    static private FaxProperties faxProperties = FaxProperties.getInstance();
    
    static public final String HYLAFAX = "HYLAFAX";
    
    
    static public FaxService create() {
	
	String service = faxProperties.getProperty(FaxProperties.SERVICE);
	
	if( HYLAFAX.equalsIgnoreCase(service) ) {
	    return new HylaFaxService();
	}
	
	return null;
	
    }
}
