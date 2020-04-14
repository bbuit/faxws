/**
 * Copyright (c) 2015-2019. The Pharmacists Clinic, Faculty of Pharmaceutical Sciences, University of British Columbia. All Rights Reserved.
 *
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
 * The Pharmacists Clinic 
 * Faculty of Pharmaceutical Sciences
 * University of British Columbia
 * Vancouver, British Columbia, Canada
 */

package org.srfax;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import org.apache.log4j.Logger;
import org.common.model.FaxJob;
import org.common.model.FaxJob.Direction;
import org.connection.HttpsConnection;
import org.service.FaxService;
import org.util.FaxProperties;
import org.util.MiscUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * @version May 26, 2016
 * This class manages connectivity to SRFax API.
 * Sets a HTTPS connection based on settings sent from the FaxProperties
 * Object. 
 * Future wish list:
 * Group faxing, 
 */
public class SRFaxService implements FaxService {

	private static Logger logger = MiscUtils.getLogger();
	
	public static enum Status {SUCCESS, FAILED}
	public static enum FaxType {SINGLE, BROADCAST}
	public static enum ViewedStatus {UNREAD, READ, ALL}
	public static String FAX_FORMAT = "PDF";
	public static String TIMES_REDIAL = "3";
	public static String DEFAULT_COUNTRY_CODE = "1";
	
	private HttpsConnection conn;

	public SRFaxService() {
		// default
	}

	public SRFaxService( FaxProperties properties ) {
		setConnection(properties);
	}

	/**
	 * Requires: 
	 * action=Get_FaxStatus
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * sFaxDetailsID=[FaxDetailsID returned from Queue_Fax POST]
	 * Optional: 
	 * sResponseFormat=XML or JSON 
	 * 
	 * Response:
	 * FileName, SentStatus, DateQueued, DateSent, EpochTime, ToFaxNumber, Pages, 
	 * Duration, RemoteID, ErrorCode, Size, AccountCode
	 * @throws IOException 
	 */
	@Override
	public FaxJob getFaxStatus(FaxJob faxJob) throws IOException {

		String params = "";
		JSONObject response = null;
		JSONObject result = null;
		try {

			params = authenticate(faxJob.getUser(), faxJob.getPassword()) 
					+ "&action=" + URLEncoder.encode("Get_FaxStatus", HttpsConnection.ENCODING)
					+ "&sFaxDetailsID=" + URLEncoder.encode(""+faxJob.getJobId(), HttpsConnection.ENCODING);

			response = getConnection().sendPost(params);

			logger.debug( "Fax Status Parameters: " + params );

		} catch (Exception e1) {
			logger.error("Error", e1);	
			throw new IOException();
		} finally {
			closeConnection();
		}

		if( Status.SUCCESS.name().equalsIgnoreCase( response.getString("Status") ) ) {

			result = (JSONObject) response.get("Result");
			faxJob.setStatus( getFaxJobStatus( result.getString("SentStatus") ) );

			if(result.containsKey("ErrorCode")) {
				faxJob.setStatusString(result.getString("ErrorCode"));
			}
		}

		if( Status.FAILED.name().equalsIgnoreCase( response.getString("Status") ) ) {
			faxJob.setStatus( FaxJob.STATUS.ERROR );
			faxJob.setStatusString( "Failed to retrieve fax status: " + response.getString("Result") );
		}

		return faxJob;
	}

	/**
	 * Requires: 
	 * action=Queue_Fax
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * sCallerID=Senders FAX Number (10 digits)
	 * sSenderEmail=Senders Email Address
	 * sFaxType="SINGLE" OR "BROADCAST" : only single faxes for now.
	 * sToFaxNumber= 11 digit number or up-to 50 * 11 digit numbers pipe delimited
	 *
	 * Optional: 
	 * sResponseFormat=XML or JSON
	 * sRetries=number times to redial number
	 * 
	 * Cover Page Details: 
	 * sCoverPage=Basic, Standard, Company or Personal
	 * sFaxFromHeader=From on the Fax Header Line
	 * sCPFromName=from on the cover page
	 * sCPToName=recipient name on the cover page
	 * sCPOrganization=Organization name on the cover page
	 * sCPSubject=subject line on the cover page
	 * sCPComments=comment line on the cover page
	 * 
	 * Fax file info:
	 * sFileName_x=valid file name
	 * sFileContent_x=Base64 encoding of file contents.
	 * 
	 * Response redirect:
	 * sNotifyURL=redirect for after the fax is sent
	 * 
	 * Future Dating:
	 * sQueueFaxDate=future date to send the fax (YYYY-MM-DD)
	 * sQueueFaxTime=future time to sent the fax (24 hour format)
	 * @throws IOException 
	 */
	@Override
	public FaxJob sendFax(FaxJob faxJob) throws IOException {

		String params = "";
		JSONObject response = null;
		
		// destination fax numbers need to be 11 digits. 
		String destination = faxJob.getDestination();
		destination = destination.replaceAll("[^0-9]+", "");
		destination = destination.trim();
		
		// maybe the country code is missing if the number is 10 chars.
		if( destination.length() == 10 ) {
			destination = DEFAULT_COUNTRY_CODE + destination;
		}

		try {

			params = authenticate(faxJob.getUser(), faxJob.getPassword()) 
					+ "&action=" + URLEncoder.encode("Queue_Fax", HttpsConnection.ENCODING)
					+ "&sCallerID=" + URLEncoder.encode(faxJob.getFax_line(), HttpsConnection.ENCODING)
					+ "&sSenderEmail=" + URLEncoder.encode(faxJob.getSenderEmail(), HttpsConnection.ENCODING)
					+ "&sFaxType=" + URLEncoder.encode(FaxType.SINGLE.name(), HttpsConnection.ENCODING)
					+ "&sToFaxNumber=" + URLEncoder.encode(destination, HttpsConnection.ENCODING)
					+ "&sRetries=" + URLEncoder.encode(TIMES_REDIAL, HttpsConnection.ENCODING)
					+ "&sFileName_1=" + URLEncoder.encode(faxJob.getFile_name(), HttpsConnection.ENCODING)
					+ "&sFileContent_1=" + URLEncoder.encode(faxJob.getDocument(), HttpsConnection.ENCODING) 
					;

			logger.debug( "Send Fax Parameters: " + params );

			response = getConnection().sendPost(params);

		} catch (Exception e) {
			logger.error("Error", e);
			throw new IOException();
		} finally {
			closeConnection();
		}

		if( Status.SUCCESS.name().equalsIgnoreCase( response.getString("Status") ) ) {
			faxJob.setStatus(FaxJob.STATUS.SENT);
			faxJob.setJobId( Long.parseLong( response.getString("Result") ) );
		}

		if( Status.FAILED.name().equalsIgnoreCase( response.getString("Status") ) ) {
			faxJob.setStatus(FaxJob.STATUS.ERROR);
			faxJob.setStatusString( response.getString("Result") );
		}

		return faxJob;
	}

	/**
	 * Get all the incoming faxes from the server.
	 * Requires:
	 * action=Get_Fax_Inbox
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * 
	 * Optional:
	 * sResponseFormat=XML or JSON
	 * sPeriod=ALL or RANGE. Default is ALL
	 * sStartDate=Required if RANGE selected (YYYYMMDD)
	 * sEndDate=Required if RANGE selected
	 * sViewedStatus=UNREAD or READ or ALL. Default is ALL
	 * sIncludeSubUsers=Includes all sub users faxes
	 * 
	 * Response:
	 * FileName, ReceiveStatus, Date, EpochTime, CallerID, RemoteID
	 * Pages, Size, ViewedStatus
	 * @throws RuntimeException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 */
	@Override
	public List<FaxJob> getFaxes(FaxJob faxJob) throws IOException {

		JSONObject response = null;
		List<FaxJob> faxJobs = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd/yy hh:mm aaa");

		try {
			String params = authenticate(faxJob.getUser(), faxJob.getPassword()) 
						+ "&action=" + URLEncoder.encode("Get_Fax_Inbox", HttpsConnection.ENCODING)
						+ "&sViewedStatus=" + URLEncoder.encode(ViewedStatus.ALL.name(), HttpsConnection.ENCODING)
						+ "&sIncludeSubUsers=" + URLEncoder.encode("Y", HttpsConnection.ENCODING)
						;

			response = getConnection().sendPost(params);
		} catch (Exception e) {
			faxJob.setStatusString( "Fatal error, contact support: " + e.getMessage() );
			logger.error("Error", e);	
			throw new IOException();
		} finally {
			closeConnection();
		}

		if( response != null ) {
			
			faxJobs = new ArrayList<FaxJob>();
			
			if( Status.SUCCESS.name().equalsIgnoreCase( response.getString("Status") ) ) {	
	
				JSONArray results = response.getJSONArray("Result");
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> it = results.iterator();
	
				while( it.hasNext() ) {
	
					JSONObject fax = it.next();
					String fileName = fax.getString("FileName");
					String jobID = "";
					Date dateRecieved = null;
	
					try {
						dateRecieved = simpleDateFormat.parse( fax.getString("Date") );
					} catch (ParseException e) {
						dateRecieved = new Date( System.currentTimeMillis() );
					}
	
					FaxJob job = new FaxJob();				
					job.setStatus( getFaxJobStatus( fax.getString("ReceiveStatus") ) );
					job.setStatusString( fax.getString("ReceiveStatus") );
					job.setDestination( fax.getString("RemoteID") );
					job.setFax_line( fax.getString("CallerID") );
					job.setNumPages( fax.getInt("Pages") );
					job.setStamp( dateRecieved );
	
					if( fileName.contains("|") ) {
						jobID = fileName.split("\\|")[1].trim();
					}
	
					job.setFile_name( fileName );
					job.setJobId( Long.parseLong( jobID ) );
					job.setDirection(Direction.IN);
					
					faxJobs.add(job);
				}
			}
	
			if( Status.FAILED.name().equalsIgnoreCase( response.getString("Status") ) ) {

				faxJob.setStatus( FaxJob.STATUS.ERROR );
				faxJob.setFile_name( null );
				String result = response.getString("Result");
				if( result != null ) {
					result = result.substring( 0, Math.min(result.length(), 255) );
				}
				faxJob.setStatusString( result );
				faxJob.setNumPages(0);
				faxJob.setStamp( new Date( System.currentTimeMillis() ) );
				faxJobs.add(faxJob);
			}
			
		} 
		
		return faxJobs;
	}
	
	/**
	 * Update an incoming fax to viewed so that it wont be repeatedly down loaded
	 * Requires:
	 * action=Update_Viewed_Status
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * sFaxFileName=File name returned by get_fax_inbox or get_fax_outbox
	 * sFaxDetailsID=ID number after the pipe character in the sFaxFileName
	 * sDirection=IN or OUT
	 * sMarkasViewed= Y as READ N as UNREAD
	 * 
	 * Optional:
	 * sResponseFormat=XML or JSON
	 * @throws IOException 
	 */
	public FaxJob updateViewedStatus(FaxJob faxJob, ViewedStatus viewedStatus) throws IOException {
		String params = "";
		JSONObject response = null;

		logger.debug( "Updating view status for file " + faxJob.getFile_name() );

		String status = "Y";
		
		if( viewedStatus.equals(ViewedStatus.UNREAD) ) {
			status = "N";
		}
		
		try {

			params = authenticate(faxJob.getUser(), faxJob.getPassword()) 
					+ "&action=" + URLEncoder.encode("Update_Viewed_Status", HttpsConnection.ENCODING)
					+ "&sFaxFileName=" + URLEncoder.encode(faxJob.getFile_name(), HttpsConnection.ENCODING)
					+ "&sDirection=" + URLEncoder.encode(faxJob.getDirection().name(), HttpsConnection.ENCODING)
					+ "&sMarkasViewed=" + URLEncoder.encode(status, HttpsConnection.ENCODING)
					;

			response = getConnection().sendPost(params);

		} catch (Exception e) {
			logger.error("Error", e);
			throw new IOException();
		}  finally {
			closeConnection();
		}

		if( Status.SUCCESS.name().equalsIgnoreCase( response.getString("Status") ) ) {			
			// Not sure if an Oscar viewed status should be set here.
			// For now leave it empty. This method only returns an empty string.
		}

		if( Status.FAILED.name().equalsIgnoreCase( response.getString("Status") ) ) {
			faxJob.setStatus(FaxJob.STATUS.ERROR);
			faxJob.setStatusString( response.getString("Result") );
		}

		return faxJob;
	}
	

	/**
	 * Gets an individual fax containing the fax details and document.
	 * 
	 * Requires:
	 * action=Retrieve_Fax
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * sFaxFileName=File name returned by get_fax_inbox or get_fax_outbox
	 * sFaxDetailsID=ID number after the pipe character in the sFaxFileName
	 * sDirection=IN or OUT
	 * 
	 * Optional:
	 * sSubUserID=sub account user ID
	 * sResponseFormat=XML or JSON
	 * sFaxFormat=PDF or TIF - this method is default to pdf.
	 * sMarkasViewed= "Y" or "N"
	 * 
	 * Response:
	 * Result=Base64 endoded PDF or TIF
	 * @throws IOException 
	 * 
	 */
	@Override
	public FaxJob getFax(FaxJob faxJob) throws IOException {

		String params = "";
		JSONObject response = null;

		logger.debug( "Getting fax file " + faxJob.getFile_name() );
		
		// default to the inbox if null
		if( faxJob.getDirection() == null ) {
			faxJob.setDirection(Direction.IN);
		}

		try {

			params = authenticate(faxJob.getUser(), faxJob.getPassword()) 
					+ "&action=" + URLEncoder.encode("Retrieve_Fax", HttpsConnection.ENCODING)
					+ "&sFaxFileName=" + URLEncoder.encode(faxJob.getFile_name(), HttpsConnection.ENCODING)
					+ "&sDirection=" + URLEncoder.encode(faxJob.getDirection().name(), HttpsConnection.ENCODING)
					+ "&sFaxFormat=" + URLEncoder.encode(FAX_FORMAT, HttpsConnection.ENCODING)
					;

			response = getConnection().sendPost(params);

		} catch (Exception e1) {
			logger.error("Error", e1);
			throw new IOException();
		} finally {
			closeConnection();
		}

		if( Status.SUCCESS.name().equalsIgnoreCase( response.getString("Status") ) ) {			
			faxJob.setDocument( response.getString("Result") );	
			faxJob.setStatus( getFaxJobStatus("received") );
			this.updateViewedStatus(faxJob, ViewedStatus.READ);
		}

		if( Status.FAILED.name().equalsIgnoreCase( response.getString("Status") ) ) {
			faxJob.setStatus(FaxJob.STATUS.ERROR);
			faxJob.setFile_name( null );
			String result = response.getString("Result");
			if( result != null ) {
				result = result.substring( 0, Math.min( result.length(), 255) );
			}
			faxJob.setStatusString( result );
			faxJob.setNumPages(0);
			faxJob.setStamp( new Date( System.currentTimeMillis() ) );
			faxJob.setStatusString( response.getString("Result") );
		 }

		return faxJob;
	}

	/**
	 * Delete a fax from the SRFax inbox or outbox.
	 * Currently only deletes from the remote inbox. The remote outbox should
	 * be set to purge itself.
	 *
	 * Required:
	 * action=Delete_Fax
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * sFaxFileName_x= faxFileName returned by the inbox or outbox
	 * sFaxDetailsID_x=faxDetail id after the pipe character of the fax filename
	 * sDirection="IN" or "OUT"
	 * 
	 * Optional:
	 * sResponseFormat=XML or JSON
	 * @throws IOException 
	 */
	@Override
	public boolean deleteFax(FaxJob fax) throws IOException {

		String params = "";
		JSONObject response = null;
		boolean deleted = Boolean.FALSE;
		
		// default to inbox if not provided.
		if( fax.getDirection() == null ) {
			fax.setDirection(Direction.IN);
		}

		try {

			params = authenticate(fax.getUser(), fax.getPassword()) 
					+ "&action=" + URLEncoder.encode("Delete_Fax", HttpsConnection.ENCODING)
					+ "&sFaxFileName_1=" + URLEncoder.encode(fax.getFile_name(), HttpsConnection.ENCODING)
					+ "&sDirection=" + URLEncoder.encode(fax.getDirection().name(), HttpsConnection.ENCODING)
					;

			response = getConnection().sendPost(params);

		} catch (Exception e1) {
			logger.error("Error", e1);	
			throw new IOException();
		} finally {
			closeConnection();
		}

		if( Status.SUCCESS.name().equalsIgnoreCase( response.getString("Status") ) ) {			
			fax.setStatusString( "fax deleted" );
			deleted = Boolean.TRUE;
		}

		if( Status.FAILED.name().equalsIgnoreCase( response.getString("Status") ) ) {
			fax.setStatus( FaxJob.STATUS.ERROR );
			fax.setStatusString( response.getString("Result") );
		}

		return deleted;
	}


	/**
	 * Stop a fax from executing.
	 * 
	 * Requires: 
	 * action=Stop_Fax
	 * access_id=[user account number]
	 * access_pwd=[user account password]
	 * sFaxDetailsID=[FaxDetailsID returned from Queue_Fax POST]
	 * 
	 * Optional:
	 * sResponseFormat=XML or JSON
	 * @throws IOException 
	 */
	@Override
	public boolean cancelFax(String user, String passwd, String jobId) throws IOException {
		String params = "";
		JSONObject response = null;
		boolean stopped = Boolean.FALSE;

		try {

			params = authenticate(user, passwd) 
					+ "&action=" + URLEncoder.encode("Stop_Fax", HttpsConnection.ENCODING)
					+ "&sFaxDetailsID=" + URLEncoder.encode(jobId, HttpsConnection.ENCODING);
			;

			response = getConnection().sendPost(params);

		} catch (Exception e1) {
			logger.error("Error", e1);
			throw new IOException();
		} finally {
			closeConnection();
		}

		if( Status.SUCCESS.name().equalsIgnoreCase( (String) response.get("Status") ) ) {			
			stopped = Boolean.TRUE;
			logger.info( response.get("Result") + " JobID[" + jobId + "]" );
		}

		if( Status.FAILED.name().equalsIgnoreCase( (String) response.get("Status") ) ) {
			logger.warn(response.get("Result") + " JobID[" + jobId + "]" );
		}

		return stopped;
	}

	private void setConnection(String url) {
		this.conn = new HttpsConnection(url);
	}

	private void setConnection(FaxProperties properties) {
		String url = properties.getProperty( FaxProperties.HOST );
		String ssl = properties.getProperty( FaxProperties.SSL_PROTOCOL );
		String readTimeout = properties.getProperty( FaxProperties.READ_TIMEOUT, "0" ); // default at 0 to assure integer value.
		String connectTimeout = properties.getProperty( FaxProperties.CONNECT_TIMEOUT, "0" );
		
		setConnection(url);
		
		if( getConnection() != null ) {			
			getConnection().setSslProtocol(ssl);
			getConnection().setConnectTimeout( Integer.parseInt(connectTimeout) );
			getConnection().setReadTimeout( Integer.parseInt(readTimeout) );			
		}
	}

	public HttpsConnection getConnection() {
		return this.conn;
	}

	public void closeConnection() {		
		logger.info("Closing Connection");
		this.conn.close();
	}

	// Helper Methods
	private static final FaxJob.STATUS getFaxJobStatus(final String status) {
		switch( status ) {
		case "In Progress" : return FaxJob.STATUS.WAITING;
		case "Ok" : return FaxJob.STATUS.RESOLVED;
		case "received" : return FaxJob.STATUS.RECEIVED;
		case "Sent" : return FaxJob.STATUS.COMPLETE;
		case "Failed" : return FaxJob.STATUS.ERROR;
		case "Sending Email" : return FaxJob.STATUS.ERROR;
		}
		return FaxJob.STATUS.UNKNOWN;
	}

	private static final String authenticate(final String user, final String pass) 
			throws UnsupportedEncodingException, WebApplicationException {
		
		if(  user == null || user.isEmpty() ) {
			logger.error("Missing or corrupted login id");
			throw new WebApplicationException(401);
		}
		
		if( pass == null || pass.isEmpty() ) {
			logger.error("Missing or corrupted password");
			throw new WebApplicationException(401);
		}
		
		return new String( "access_id=" + URLEncoder.encode( user.trim(), HttpsConnection.ENCODING ) 
				+ "&access_pwd=" + URLEncoder.encode( pass.trim(), HttpsConnection.ENCODING ) );

	}

}
