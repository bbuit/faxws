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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MiscUtils {

    /**
     * 
     * This method will return a logger instance which has the name based on the class that's calling this method.
     */
    public static Logger getLogger()
    {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        String caller = ste[2].getClassName();
        return LogManager.getLogger(caller);
    }
	
}
