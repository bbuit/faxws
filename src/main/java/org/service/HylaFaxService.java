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
package org.service;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.io.ByteArrayOutputStream;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.log4j.Logger;
import org.common.model.FaxJob;
import org.util.FaxProperties;
import org.util.MiscUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import gnu.hylafax.ClientProtocol;
import gnu.hylafax.HylaFAXClient;
import gnu.hylafax.Client;
import gnu.hylafax.Job;
import gnu.hylafax.Pagesize;
import gnu.inet.ftp.FtpClient;
import gnu.inet.ftp.FtpClientProtocol;
import gnu.inet.ftp.ServerResponseException;

public class HylaFaxService implements FaxService {
    
    private final static Logger log = MiscUtils.getLogger();
    private static Integer FAXLINE = 0;
    private static Integer FAXDIR = 1;
    private static Integer PORT = 4559; 
    private static String[] DATE_FORMATS = {"ddMMMyy","EEEhha", "HH:mm"}; //Date formats returned by hylafax for file listings    
    private static String killtime = "000259";
    private static int maxdials = 12;
    private static int maxtries = 3;
    private static int priority = 127;
    private static int resolution = Integer.parseInt(FaxProperties.getInstance().getProperty("RESOLUTION","98"));
    private static String notify = "none";
    private static int chopthreshold = 3;
    private static String TMPDIR = System.getProperty("java.io.tmpdir","/tmp");
    private static String TIFF2PDF =  "/usr/bin/tiff2pdf -p letter -j -q 100 -f -o ";
    private static Dimension pagesize = Pagesize.LETTER;
   
    
    
    public boolean cancelFax(String user, String passwd, String jobId) {
	
	HylaFAXClient c = new HylaFAXClient();
	boolean result = false;
	
	try {
	    FaxProperties faxProperties = FaxProperties.getInstance();
	    String host = faxProperties.getProperty(FaxProperties.HOST);	
		
	    if( host == null ) {
		log.error("NO HYLAFAX HOST CONFIGURED");
		throw new WebApplicationException(500);
	    }
	    
	    c.setSocketTimeout(60000);
	    
	    c.open(host);
	    c.user(user);

	    try {
		c.pass(passwd);
	    }
	    catch(ServerResponseException e) {
		//already logged in error
		if( e.getCode() != 503 ) {
		    throw e;
		}
		    
	    }
	    
	    Job job = c.getJob(Long.parseLong(jobId));
	    
	    c.suspend(job);
	    c.delete(job);
	    
	    result = true;
	
	} catch (IOException | ServerResponseException | WebApplicationException e) {
	    log.error("PROBLEM WITH CONNECTING TO HYLAFAX", e);
	}
	finally {
	    try {
		c.quit();
	    } catch (IOException | ServerResponseException e) {
		log.error(e.getMessage(), e);
	    }
	}
	
	return result;
    }
        
    public FaxJob getFaxStatus(FaxJob faxJob) {
	
	
	
	HylaFAXClient c = new HylaFAXClient();
	
	try {
	    FaxProperties faxProperties = FaxProperties.getInstance();
	    String host = faxProperties.getProperty(FaxProperties.HOST);	
		
	    if( host == null ) {
		log.error("NO HYLAFAX HOST CONFIGURED");
		throw new WebApplicationException(500);
	    }
	    
	    c.open(host);
	    c.user(faxJob.getUser());

	    try {
		c.pass(faxJob.getPassword());
	    }
	    catch(ServerResponseException e) {
		if(e.getCode() != 503 ) {
		    throw e;
		}
	    }
	    
	    Job job = c.getJob(faxJob.getJobId());
	    String state = job.getProperty("STATE");
	    
	    log.info("STATUS " + state);
	    if( state.equalsIgnoreCase("FAILED") ) {		
		faxJob.setStatus(FaxJob.STATUS.ERROR);
		faxJob.setStatusString(job.getProperty("STATUS"));
	    }
	    else if( state.equalsIgnoreCase("DONE")){
		faxJob.setStatus(FaxJob.STATUS.COMPLETE);
	    }
	    else {
		faxJob.setStatus(FaxJob.STATUS.WAITING);
	    }
	    
	    
	} catch (IOException | ServerResponseException | WebApplicationException e) {
	    log.error("PROBLEM WITH CONNECTING TO HYLAFAX", e);	    
	    return null;
	}
	finally {
	    try {
		c.quit();
	    } catch (IOException | ServerResponseException e) {
		log.error(e.getMessage(), e);
	    }
	}
	
	return faxJob;
	    
    }
	    
    public FaxJob sendFax(FaxJob faxJob) {
	
	HylaFAXClient c = new HylaFAXClient();
	
	try {
	    FaxProperties faxProperties = FaxProperties.getInstance();
	    String host = faxProperties.getProperty(FaxProperties.HOST);	
		
	    if( host == null ) {
		log.error("NO HYLAFAX HOST CONFIGURED");
		throw new WebApplicationException(500);
	    }
	    
	    c.open(host);
	    c.user(faxJob.getUser());
	    
	    try {
		c.pass(faxJob.getPassword());
	    }
	    catch(ServerResponseException e) {
		if( e.getCode() != 503 ) {
		    throw e;
		}
	    }
	    
	    c.tzone(ClientProtocol.TZONE_LOCAL);
	    
	    ByteArrayInputStream pdf = new ByteArrayInputStream(Base64Utility.decode(faxJob.getDocument()));
	    
	    c.mode(FtpClientProtocol.MODE_ZLIB);
	    c.type(FtpClientProtocol.TYPE_IMAGE);
	    
	    String remote_filename = c.putTemporary(pdf);
	    
	    Job job = c.createJob();
	    
	    job.setFromUser(faxJob.getFax_line());
	    job.setKilltime(killtime);
	    job.setMaximumDials(maxdials);
	    job.setMaximumTries(maxtries);
	    job.setPriority(priority);
	    job.setDialstring(faxJob.getDestination());
	    job.setVerticalResolution(resolution);
	    job.setPageDimension(pagesize);
	    job.setNotifyType(notify);
	    job.setChopThreshold(chopthreshold);
	    job.addDocument(remote_filename);
	    
	    c.submit(job);
	    
	    faxJob.setDocument(null);
	    faxJob.setJobId(job.getId());
	    
	} catch (IOException | ServerResponseException | WebApplicationException e) {
	    log.error("PROBLEM WITH CONNECTING TO HYLAFAX", e);	    
	    return null;
	} catch (Base64Exception e) {
	    log.error("PROBLEM DECODING PDF");
	    return null;
	}
	finally {
	    try {
		c.quit();
	    } catch (IOException | ServerResponseException e) {
		log.error("COMM ERROR WITH HYLAFAX",e);
	    }
	}
	
	return faxJob;
    }
    
    public boolean deleteFax(FaxJob fax) {
	FtpClient ftpClient = new FtpClient();
	try {
	    FaxProperties faxProperties = FaxProperties.getInstance();
	    String host = faxProperties.getProperty(FaxProperties.HOST);	
		
	    if( host == null ) {
		log.error("NO HYLAFAX HOST CONFIGURED");
		throw new WebApplicationException(500);
	    }
	    ftpClient.open(host, PORT);	    
	    ftpClient.user(fax.getUser());

	    try {
		ftpClient.pass(fax.getPassword());
	    }
	    catch(ServerResponseException e) {
		if(e.getCode() != 503 ) {
		    throw e;
		}
	    }
	    
	    
	    
	    String faxLineDir = faxProperties.getProperty(FaxProperties.FAXLINES);
	    if( faxLineDir == null ) {
		log.error("NO HYLAFAX FAX DIRECTORY SPECIFIED");
		throw new WebApplicationException(500);
	    }
		
	    String directory = null;
	    String faxLines[] = faxLineDir.split(",");	
	    String[] faxLine;
	    for( int idx = 0; idx < faxLines.length; ++idx ) {
		faxLine = faxLines[idx].trim().split(" ");
		if( faxLine.length != 2 ) {
		    continue;
		}
		    
		if( faxLine[FAXLINE].equals(fax.getFax_line()) ) {
		    directory = faxLine[FAXDIR];
		    break;
		}
	    }
		
	    if( directory == null ) {
		log.error("FAX LINE NOT CONFIGURED");
		throw new WebApplicationException(500);
	    }
	    
	    ftpClient.dele(directory + "/" + fax.getFile_name());
	    
	    
	} catch (IOException | ServerResponseException e) {
	    log.error("PROBLEM WITH CONNECTING TO HYLAFAX", e);
	    return false;
	}
	finally {
	    try {
		ftpClient.quit();
	    } catch (IOException | ServerResponseException e) {
		log.error("PROBLEM CONNECTING TO HYLAFAX", e);
	    }
	}
	
	return true;
    }

    public List<FaxJob> getFaxes(FaxJob faxJob) {
	FaxProperties faxProperties = FaxProperties.getInstance();
	
	Client hylafaxClient = new HylaFAXClient();
	String host = faxProperties.getProperty(FaxProperties.HOST);	
	
	if( host == null ) {
	    log.error("NO HYLAFAX HOST CONFIGURED");
	    throw new WebApplicationException(500);
	}
	
	String user = faxJob.getUser();
	if( user == null ) {
	    log.error("NO HYLAFAX USER DEFINED");
	    throw new WebApplicationException(401);
	}
	
	String passwd = faxJob.getPassword();
	if( passwd == null ) {
	    log.error("NO HYLAFAX PASSWORD CONFIGURED");
	    throw new WebApplicationException(401);
	}
	
	String faxLineDir = faxProperties.getProperty(FaxProperties.FAXLINES);
	if( faxLineDir == null ) {
	    log.error("NO HYLAFAX FAX DIRECTORY SPECIFIED");
	    throw new WebApplicationException(500);
	}
	
	String directory = null;
	String faxLines[] = faxLineDir.split(",");	
	String[] faxLine;
	for( int idx = 0; idx < faxLines.length; ++idx ) {
	    faxLine = faxLines[idx].trim().split(" ");
	    if( faxLine.length != 2 ) {
		continue;
	    }
	    
	    if( faxLine[FAXLINE].equals(faxJob.getFax_line()) ) {
		directory = faxLine[FAXDIR];
		break;
	    }
	}
	
	if( directory == null ) {
	    log.error("FAX LINE NOT CONFIGURED");
	    throw new WebApplicationException(500);
	}
	
	try {
	    
	    
	    hylafaxClient.open(host);
	    hylafaxClient.user(user);

	    try {
		hylafaxClient.pass(passwd);
	    }
	    catch(ServerResponseException e) {
		if(e.getCode() != 503 ) {
		    throw e;
		}
	    }
	    
	    return getListings(faxJob, directory, hylafaxClient);
	    
	    
	}
	catch( UnknownHostException e ) {
	    log.error("UNKNOWN HYLAFAX HOST",e);
	    throw new WebApplicationException(500);
	}
	catch( FileNotFoundException e ) {
	    log.error("FILE NOT FOUND", e);
	    throw new WebApplicationException(404);
	}
	catch( ServerResponseException e ) {
	    if( e.getCode() == 503 ) {
		try {
		    return getListings(faxJob, directory, hylafaxClient);
		}
		catch( FileNotFoundException fnfE ) {
		    log.error("FILE NOT FOUND", fnfE);
		    throw new WebApplicationException(404);
		}
		catch( IOException ex ) {
		    log.error("IO ERROR WITH HYLAFAX", ex);
		    throw new WebApplicationException(500);
		}
		catch( ServerResponseException ex ) {
		    log.error("HYLAFAX RETURNED ERROR", ex);
		    throw new WebApplicationException(500);
		}
	    }
	    else {
		log.error("HYLAFAX RETURNED ERROR", e);
		throw new WebApplicationException(500);
	    }
	}
	catch( IOException e ) {
	    log.error("IO ERROR WITH HYLAFAX", e);
	    throw new WebApplicationException(500);
	}
			
    }
    
    @SuppressWarnings("unchecked")
    private List<FaxJob>getListings(FaxJob faxJob, String directory, Client hylafaxClient) throws FileNotFoundException, IOException, ServerResponseException {
	List<FaxJob> listings = new ArrayList<FaxJob>();
	if( faxJob.getFile_name() != null ) {
			    
	    	ByteArrayOutputStream tiffBytes = new ByteArrayOutputStream();
	    	
	    	hylafaxClient.type(FtpClientProtocol.TYPE_IMAGE);
	    	hylafaxClient.get(directory + "/" + faxJob.getFile_name(), tiffBytes);
	    	
	    	Process process;
	    	
	    	try {
        	    	ByteArrayInputStream tiffInput = new ByteArrayInputStream(tiffBytes.toByteArray());
        	    	
        	    	
        	    	String tmpTiffFaxFilename = TMPDIR + "/tmpTiffFax" + String.valueOf(Math.random() * 10000) + ".tif";
        	    	String tmpPdfFaxFilename =  TMPDIR + "/tmpTiffFax" + String.valueOf(Math.random() * 10000) + ".pdf";
        	    	
        	    	File tmpTiffFaxFile = new File(tmpTiffFaxFilename);
        	    	FileOutputStream outStream = new FileOutputStream(tmpTiffFaxFile);
        	    	
        	    	IOUtils.copy(tiffInput, outStream);
        	    	
        	    	outStream.close();
        	    	
        	    	String tiff2pdfCmd = TIFF2PDF + tmpPdfFaxFilename + " " +  tmpTiffFaxFilename;
        	    	
        	    	process = Runtime.getRuntime().exec(tiff2pdfCmd);
        	    	int ret = -1;
        	    	
        	    	while( ret < 0  ) {
        	    	    try {
        	    		ret = process.waitFor();
        	    	    }
        	    	    catch( InterruptedException e ) {
        	    		
        	    	    }
        	    	}
        	    		
        	    	log.debug("tiff2pdf returned " + ret);
        	    	
        	    	ByteArrayOutputStream pdfBytesStream = new ByteArrayOutputStream();
        	    	File tmpPdfFile = new File(tmpPdfFaxFilename);
        	    	IOUtils.copy(new FileInputStream(tmpPdfFile), pdfBytesStream);
        	    	
        	    	tmpPdfFile.delete();
        	    	tmpTiffFaxFile.delete();
        	    	
    	        	FaxJob faxFile = new FaxJob();
    	        	String base64 = Base64Utility.encode(pdfBytesStream.toByteArray());
    	    		
    	        	faxFile.setDocument(base64);
    	        	faxFile.setFile_name(faxJob.getFile_name());
    	        	listings.add(faxFile);
		}
        	catch(Exception docex ) {
        	    log.error("ERROR", docex);
        	    //throw new WebApplicationException(500);
        	}
		
	    }
	    else {
		
		String listfmt = "%4p%1z;%-8.8o;%14.14s;%7t;%f";
		
		hylafaxClient.rcvfmt(listfmt);
		Vector<String> faxes = hylafaxClient.getList(directory);
		
		String []tmp;
		FaxJob outgoing;
		Calendar stamp;		
		Calendar now = GregorianCalendar.getInstance();
		SimpleDateFormat fmt = new SimpleDateFormat();
		Date d = null;
		
			
		for(int idx = 0; idx < faxes.size(); ++idx) {
		    log.debug("LISTING '" + faxes.elementAt(idx) + "'");
		    tmp = faxes.elementAt(idx).trim().split(";",-1);
		    
		    if( !tmp[1].trim().equalsIgnoreCase(faxJob.getUser())) {
			continue;
		    }
		    
		    outgoing = new FaxJob();
		    outgoing.setStatus(FaxJob.STATUS.RECEIVED);
		    
		    for( int fieldCounter = 0; fieldCounter < 5; ++fieldCounter ) {
			
			switch( fieldCounter ) {				
			case 0:
			    
			    String numPages = tmp[fieldCounter].trim();
			    log.debug("NUMPAGES " + numPages);
			    outgoing.setNumPages( Integer.parseInt( numPages ) );
			    
			    break;
			    
			case 1:
			    
			    String user = tmp[fieldCounter].trim();
			    
			    log.debug("USER: " + user);
			    outgoing.setUser(user.toString());
			    break; 
			    
			case 2:

			    String phoneNumber = tmp[fieldCounter].trim();
			    
			    log.debug("PHONE: " + phoneNumber);
			    
			    if( phoneNumber != null  && !phoneNumber.equals("")) {
				int numDigits = 0;
				StringBuilder phone = new StringBuilder();
				for ( int c = 0; c < phoneNumber.length() && numDigits <= 11; ++c  ) {
				    
				    if( StringUtils.isNumeric( String.valueOf( phoneNumber.charAt(c) ) ) ) {
					phone.append(phoneNumber.charAt(c));
					++numDigits;
				    }
				    
				}
				outgoing.setDestination(phone.toString());
			    }
			    
			    break;
			    
			case 3:
			  //Hylafx returns 3 possible formats for dates of faxes received.  We need to handle all three differently

			    String time = tmp[fieldCounter].trim();
			    
			    log.debug("TIME: " + time);
			    stamp = GregorianCalendar.getInstance();	
			    
			    int counter = 0;
			    d = null;
			    while(d == null && counter < DATE_FORMATS.length) {
								
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				fmt.applyPattern(DATE_FORMATS[counter]);				
								
				d = fmt.parse(time, new ParsePosition(0));
				log.info("d IS NULL " + String.valueOf(d == null));
				++counter;
			    }
			    
			    
			    //Date fromGmt = new Date(d.getTime() + TimeZone.getDefault().getOffset(now.getTimeInMillis()));	
			    stamp.setTime(d);
			    
			    
			    int today_of_month = now.get(Calendar.DAY_OF_MONTH);
			    int today_month = now.get(Calendar.MONTH);
			    int today_year = now.get(Calendar.YEAR);
			    
			    int today_of_week = now.get(Calendar.DAY_OF_WEEK);
			    int fax_day_of_week = stamp.get(Calendar.DAY_OF_WEEK);
			    
			    if( counter == 1 ) {
				stamp.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
				stamp.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
			    }			    
			    else if( counter == 2 ) {
				
				
				log.info("SETTING STAMP TO THIS MONTH");
				
				int dayDelta;
	    			if( fax_day_of_week > today_of_week ) {
	    			    dayDelta = fax_day_of_week - today_of_week - 7;
	    			}
	    			else {
	    			    dayDelta = (today_of_week - fax_day_of_week) * -1;
	    			}
	    				
	    			int month_delta = today_of_month + dayDelta;
	    				
	    			if( month_delta < 1 ) {				    
	    			    --today_month;
	    			    if( today_month < Calendar.JANUARY ) {
	    				today_month = Calendar.DECEMBER;
	    				--today_year;
	    			    }				    				    
	    			}
	    				
	    			if( month_delta < 1 ) {
	    			    today_of_month = stamp.getActualMaximum(Calendar.DAY_OF_MONTH) + month_delta;  //month_delta is 0 or negative so add it to max number of days for the month
	    			}
	    			else {
	    			    today_of_month = month_delta;
	    			}

				
				stamp.set(Calendar.MONTH, today_month);
				
				stamp.set(Calendar.DAY_OF_MONTH, today_of_month);
				stamp.set(Calendar.YEAR, today_year);												
				
			    }
			    else if( counter == 3 ) {
				log.info("SETTING STAMP TO TODAY");
				
				stamp.set(Calendar.DAY_OF_MONTH, today_of_month);
				stamp.set(Calendar.MONTH, today_month);
				stamp.set(Calendar.YEAR, today_year);
				
			    }
			    			    			    
			    outgoing.setStamp(stamp.getTime());
			    
			    break;			
			    
			case 4:
			    
			    String filename = tmp[fieldCounter].trim();
			    log.debug("FILENAME: " + filename);
			    outgoing.setFile_name(filename);
			    break;
			}
			
			
			
		    }
		    
		    listings.add(outgoing);
		}
	    }
	
	hylafaxClient.quit();
	return listings;
    }

}
