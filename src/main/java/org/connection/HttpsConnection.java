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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.Logger;
import org.util.MiscUtils;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


/**
 * @version June 30, 2016
 * This class manages connectivity to secure web services. 
 */
public class HttpsConnection {
	
	private static Logger logger = MiscUtils.getLogger();
	
	protected static final int DEFAULT_READ_TIMEOUT = 6000;
	protected static final int DEFAULT_CONNECT_TIMEOUT = 6000;
	protected static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";
	
	public static final int UNPARSABLE_DATA = 422;
	public static final int SUCCESS = 201;
	public static final int NOT_AUTHORIZED = 401;
	public static final int FORBIDDEN = 403;
	public static final String ENCODING = "UTF-8";	
	
	private static enum Action {POST, GET};
	private static String SECURE_SERVICE = "https";

	private URL url;
	private HttpsURLConnection scon;
	private InputStream in;
	private OutputStream out;
	private BufferedReader bufferedReader;
	private int responseCode;
	private String sslProtocol;
	private int readTimeout;
	private int connectTimeout;
	
	public HttpsConnection(String uri) {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookieManager);
		this.setUrl(uri);
	}
	
	/**
	 * Sends post data in a single line of post parameters.
	 * Returns a JSON object.
	 */
	public JSONObject sendPost(String params) throws IOException, NoSuchAlgorithmException, 
		RuntimeException, KeyManagementException {		
		return sendPost(null, params);		
	}

	/**
	 * Only use this method if the web service client requires authentication in 
	 * the authentication header.  Otherwise group the authentication parameters
	 * with all the other parameters.
	 */
	public JSONObject sendPost(String auth, String input) throws IOException, 
		NoSuchAlgorithmException, RuntimeException, KeyManagementException {
		
		execute( getUrl(), Action.POST, auth, input );
		return this.jsonWriter( in );
	}
	
	/**
	 * Sends get data in a single line of post parameters.
	 * Returns a JSON object.
	 */
	public JSONObject sendGet(String auth, String input) throws IOException, KeyManagementException, 
		NoSuchAlgorithmException, RuntimeException {
		
		execute( getUrl(), Action.GET, auth, input );		
		return this.jsonWriter( in );
	}


	private void execute(URL uri, Action method, String auth, String input) 
			throws IOException, RuntimeException, KeyManagementException, NoSuchAlgorithmException {

		if( uri != null ) {

			logger.debug("Opening connection to " + uri.toString() );
			
			scon = ( HttpsURLConnection ) uri.openConnection();
			SSLContext sc = SSLContext.getInstance( getSslProtocol() );
			sc.init( null, null, new java.security.SecureRandom() );
			scon.setSSLSocketFactory( sc.getSocketFactory() );
			scon.setConnectTimeout( getConnectTimeout() );
			scon.setReadTimeout( getReadTimeout() );
			scon.setRequestMethod( method.name() );
			scon.setRequestProperty("Accept", "application/json");

			if(auth != null) {
				String encoded = Base64.getEncoder().encodeToString(auth.getBytes());
				scon.setRequestProperty("Authorization", "Basic " + encoded );
			}

			if( method.equals(Action.POST) ) {				
				in = doSSLPost(scon, input);
			}
			
			if( method.equals(Action.GET) ) {
				in = doSSLGet(scon);
			}			
		}		        	    	
	}

	private InputStream doSSLGet(HttpsURLConnection scon) throws IOException, RuntimeException {		
		scon.setDoInput(Boolean.TRUE);		
		setResponseCode( scon.getResponseCode() );
		return scon.getInputStream();
	}

	private InputStream doSSLPost(HttpsURLConnection scon, String input) throws IOException, RuntimeException {
		scon.setUseCaches(false);
		scon.setDoInput(true);
		scon.setDoOutput(true);

		out = scon.getOutputStream();
		out.write( input.getBytes( ENCODING ) );	

		setResponseCode(scon.getResponseCode());
		return scon.getInputStream();
	}
	

	public void close() {
		
		if(out != null) {
			try {
				out.close();
				out = null;
			} catch (IOException e) {
				logger.error("Error closing OutputStream ", e);
			}
		}
		
		if( in != null ) {
			try {
				in.close();
				in = null;
			} catch (IOException e) {
				logger.error("Error closing InputStream ", e);
			}
		}
		
		if( bufferedReader != null ) {
			try {
				bufferedReader.close();
				bufferedReader = null;
			} catch (IOException e) {
				logger.error("Error closing BufferedReader ", e);
			}
		}
		
		if( scon != null ) {
			logger.debug("Closing connection to " + getUrl().toString() );
			scon.disconnect();
			scon = null;
		}		
	}
	
	public URL getUrl() {
		return url;
	}

	public String getSslProtocol() {
		if( sslProtocol == null || sslProtocol.isEmpty() ) {
			return DEFAULT_SSL_PROTOCOL;
		}

		return sslProtocol;
	}

	public void setSslProtocol(String sslProtocol) {
		this.sslProtocol = sslProtocol;
	}

	public void setUrl(final String url) {
		try {			
			this.url = checkProtocol(url);
		} catch (MalformedURLException e) {
			logger.error("Fatal Error", e);
		}
	}
	
	private final URL checkProtocol(String url) throws MalformedURLException {
	
		int protocolIndex = url.indexOf(":");
		String protocol = url.substring(0, protocolIndex);

		URL newLink = null;	
		
		if( protocol.equalsIgnoreCase(SECURE_SERVICE) ) {
			newLink = new URL(url);
		}	
		
		return newLink;
	}
	
	private JSONObject jsonWriter( InputStream inputStream ) throws IOException {
		JSONObject jsonArray = null; 
		String inputString;
		BufferedReader bufferedReader;
		
		if(inputStream != null) {
			bufferedReader = new BufferedReader( new InputStreamReader( in, ENCODING ) );
			while ( (inputString = bufferedReader.readLine()) != null ) {				
				logger.debug(inputString);
				jsonArray = (JSONObject) JSONSerializer.toJSON(inputString);
			}
		}
		
		return jsonArray;
	}

	public int getResponseCode() {
		return responseCode;
	}

	private void setResponseCode(int responseCode) throws RuntimeException  {
		this.responseCode = responseCode;
		this.checkResponse(responseCode);
	}
	
	private void checkResponse(int responseCode) throws RuntimeException  {
		if( responseCode > 399 ) {			
			logger.error("AN ERROR OCCURRED. RESPONSE CODE: "+responseCode);			
			this.close();			
			throw new RuntimeException( "Failed : HTTP error code : " + responseCode );
		}
	}

	protected int getReadTimeout() {
		if( this.readTimeout > 0 ) {
			return readTimeout;
		}
		return DEFAULT_READ_TIMEOUT;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	protected int getConnectTimeout() {
		
		if( this.connectTimeout > 0 ) {
			return this.connectTimeout;
		}
		return DEFAULT_CONNECT_TIMEOUT;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
