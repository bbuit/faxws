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
package org.connection;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.util.FaxProperties;

/**
 * @version May 26, 2016
 * Tests to see if the main SRFax link is alive and well.
 *  
 */
public class HttpsConnectionTest {

	static HttpsConnection connection;
	static String readTimeout;
	static String connectTimeout;
	static String ssl;
	
	@BeforeClass
	public static void setUp() throws Exception {
		
		FaxProperties faxProperties =  FaxProperties.getInstance();
		String url = faxProperties.getProperty("HOST");
		
		ssl = faxProperties.getProperty( FaxProperties.SSL_PROTOCOL );		
		readTimeout = faxProperties.getProperty( FaxProperties.READ_TIMEOUT, "0" );
		connectTimeout = faxProperties.getProperty( FaxProperties.CONNECT_TIMEOUT, "0" );
		
		connection = new HttpsConnection(url);	
		connection.setSslProtocol(ssl);
		connection.setConnectTimeout( Integer.parseInt(connectTimeout) );
		connection.setReadTimeout( Integer.parseInt(readTimeout) );
	}
	
	@AfterClass
	public static void tearDown() {
		readTimeout = null;
		connectTimeout = null;
		ssl = null;
		connection.close();
	}

	@Test
	public void testPull() {
		try {
			connection.sendPost("");
			Assert.assertEquals(200, connection.getResponseCode());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetConnectTimeout() {
		int expected = Integer.parseInt( connectTimeout );
		if( expected == 0 ) {
			expected = HttpsConnection.DEFAULT_CONNECT_TIMEOUT; // expected default value
		}
		int actual = connection.getConnectTimeout();
		Assert.assertEquals( expected, actual );
	}
	
	@Test
	public void testGetReadTimeout() {
		int expected = Integer.parseInt( readTimeout );
		if( expected == 0 ) {
			expected = HttpsConnection.DEFAULT_READ_TIMEOUT; // expected default value
		}
		int actual = connection.getReadTimeout();
		Assert.assertEquals( expected, actual );
	}
	
	@Test
	public void testGetSSLProtocol() {
		String actual = connection.getSslProtocol();
		String expected = ssl;
		if( expected == null || expected.isEmpty() ) {
			expected = HttpsConnection.DEFAULT_SSL_PROTOCOL;
		}

		Assert.assertEquals( expected, actual );
	}


}
