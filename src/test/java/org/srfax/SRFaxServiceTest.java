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
package org.srfax;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.geronimo.mail.util.Base64;
import org.common.model.FaxJob;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.service.FaxServiceFactory;

/**
 * @version May 26, 2016
 * Tests for the various RESTful endpoints published by 
 * SRFax.  
 * 
 * This test case is intended for troubleshooting use only. 
 * Cannot be used as a general test case because account
 * credentials are required first.
 *  
 */
public class SRFaxServiceTest {

	private static SRFaxService faxService;
	private static String username = "";
	private static String password = "";
	private static String faxline = "";
	
	@Before
	public void setUp() throws Exception {
		faxService = (SRFaxService) FaxServiceFactory.create();
	}

	@After
	public void tearDown() throws Exception {
		faxService.closeConnection();
		faxService = null;
	}
	
	// @Test
	public void testGetFaxStatus() {

		FaxJob faxJob2 = new FaxJob();
		faxJob2.setUser(username);
		faxJob2.setPassword(password);
		faxJob2.setJobId(new Long(293643251));

		try {
			faxService.getFaxStatus(faxJob2);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(200, faxService.getConnection().getResponseCode());
	}
	
	// @Test
	public void testFailedGetFaxStatus() {

		FaxJob faxJob = new FaxJob();
		faxJob.setUser(username);
		faxJob.setPassword(password);
		faxJob.setJobId(new Long(12));
		try {
			faxService.getFaxStatus(faxJob);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Assert.assertEquals(200, faxService.getConnection().getResponseCode());
	}

	// @Test
	public void testSendFax() {
		System.out.println("testSendFax()");
		FaxJob faxJob = new FaxJob();
		faxJob.setUser(username);
		faxJob.setPassword(password);
		faxJob.setSenderEmail("dwarren@colcamex.com");
		faxJob.setDestination("-(604) 685-9264");
		faxJob.setFile_name("SRFax-Fax-API-Documentation.pdf");
		faxJob.setDocument(encodeFileToBase64("/SRFax-Fax-API-Documentation.pdf"));
		faxJob.setFax_line(faxline);
		try {
			System.out.println(faxService.sendFax(faxJob));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testGetFaxes() {
		
		System.out.println("testGetFaxes()");
		FaxJob faxJob = new FaxJob();
		faxJob.setUser(username);
		faxJob.setPassword(password);		
		try {
			System.out.println(faxService.getFaxes(faxJob));

		} catch (IOException e) {
			e.printStackTrace();
		} 

	}
	
	//@Test
	public void testGetFax() {
		System.out.println("testGetFax()");
		FaxJob faxJob = new FaxJob();
		faxJob.setUser(username);
		faxJob.setPassword(password);
		faxJob.setFile_name("20160504110220-3668-12_1|291429401");
		try {
			System.out.println(faxService.getFax(faxJob));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testDeleteFax() {
		System.out.println("testDeleteFax()");
		FaxJob faxJob = new FaxJob();
		faxJob.setUser(username);
		faxJob.setPassword(password);
		faxJob.setFile_name("20160520153257-3164-12_1");
		faxJob.setJobId(Long.parseLong("292462728") );
		try {
			System.out.println( faxService.deleteFax(faxJob) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testCancelFax() {
		System.out.println("testCancelFax()");
		try {
			System.out.println(faxService.cancelFax(username, password, "293643251"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String encodeFileToBase64(String fileName) {

		InputStream is = this.getClass().getResourceAsStream(fileName);
		byte[] bytes = null;
		try {
			bytes = loadFile(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] encoded = Base64.encode(bytes);
		String encodedString = new String(encoded);

		return encodedString;
	}
	
	private static byte[] loadFile(InputStream is) throws IOException {

	    long length = is.available();
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }
	    byte[] bytes = new byte[(int)length];
	    
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file ");
	    }


	    return bytes;
	}

}
