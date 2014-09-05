package org.util;

import org.apache.log4j.Logger;

public class MiscUtils {

    /**
     * 
     * This method will return a logger instance which has the name based on the class that's calling this method.
     */
    public static Logger getLogger()
    {
	StackTraceElement[] ste = Thread.currentThread().getStackTrace();
	String caller = ste[2].getClassName();
	return(Logger.getLogger(caller));
    }
	
}
