package org.service;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.io.ByteArrayOutputStream;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.log4j.Logger;
import org.common.model.FaxJob;
import org.util.FaxProperties;
import org.util.MiscUtils;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.codec.TiffImage;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
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
    private static Integer FIELDS = 6;
    private static Integer PORT = 4559; 
    private static String[] DATE_FORMATS = {"ddMMMyy","EEEhha", "HH:mm"}; //Date formats returned by hylafax for file listings    
    private static String killtime = "000259";
    private static int maxdials = 12;
    private static int maxtries = 3;
    private static int priority = 127;
    private static int resolution = Integer.parseInt(FaxProperties.getInstance().getProperty("RESOLUTION","98"));
    private static String notify = "none";
    private static int chopthreshold = 3;
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
	    	
	    	ByteArrayInputStream tiffInput = new ByteArrayInputStream(tiffBytes.toByteArray());
	    	
	    	//Read the Tiff File
	        RandomAccessFileOrArray ra = new RandomAccessFileOrArray(tiffInput);
	        //Find number of images in Tiff file
	        int numberOfPages = TiffImage.getNumberOfPages(ra);
	        
	        try {
    	        	Document TifftoPDF = new Document(PageSize.LETTER);
    	        	ByteArrayOutputStream pdfBytes = new ByteArrayOutputStream();
    	        	PdfWriter pdfWriter =  PdfWriter.getInstance(TifftoPDF, pdfBytes);
    	        	        
    	        	TifftoPDF.open();
    	        	//Run a for loop to extract images from Tiff file
    	        	//into a Image object and add to PDF doc
    	        
    	        	for( int i = 1; i <= numberOfPages; i++ ){
    	        	    Image tempImage = TiffImage.getTiffImage(ra, i);
    	            
    	        	    tempImage.scaleToFit(PageSize.LETTER.getWidth(), PageSize.LETTER.getHeight());
    	        	    TifftoPDF.add(tempImage);
    	            
    	        	    TifftoPDF.newPage();	            
    	        	}
    	        
    	        	TifftoPDF.close();
    	        	pdfWriter.close();
    	    			       
    	        	FaxJob faxFile = new FaxJob();
    	        	String base64 = Base64Utility.encode(pdfBytes.toByteArray());
    	    		
    	        	faxFile.setDocument(base64);
    	        	faxFile.setFile_name(faxJob.getFile_name());
    	        	listings.add(faxFile);
		}
        	catch(DocumentException docex ) {
        	    log.error("ERROR", docex);
        	    throw new WebApplicationException(500);
        	}
		
	    }
	    else {
		Vector<String> faxes = hylafaxClient.getList(directory);
		
		String[] listing;		
		FaxJob outgoing;
		Calendar stamp;
		Calendar local = GregorianCalendar.getInstance();
		Calendar now = GregorianCalendar.getInstance();
		SimpleDateFormat fmt = new SimpleDateFormat();
		Date d;
		
			
		for(int idx = 0; idx < faxes.size(); ++idx) {
		    listing = faxes.elementAt(idx).split(" ");
		    outgoing = new FaxJob();
		    outgoing.setStatus(FaxJob.STATUS.RECEIVED);
		    
		    for( int i = 0,itemIdx = 0; i < listing.length; ++i ) {
			log.info(listing[i]);
			if( StringUtils.trimToNull(listing[i]) == null ) {
			    continue;
			}
			++itemIdx;
									
			switch(itemIdx%FIELDS) {
			case 0:
			    outgoing.setFile_name(listing[i]);
			    break;					
			case 2:
			    outgoing.setNumPages(Integer.parseInt(listing[i]));
			    break;
			case 3:
			    outgoing.setUser(listing[i]);
			    break;
			case 4:
			    outgoing.setDestination(listing[i]);
			    break;
			case 5:
			    //Hylafx returns 3 possible formats for dates of faxes received.  We need to handle all three differently
			    stamp = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));			    
			    int counter = -1;
			    do {
				
				++counter;
				fmt.applyPattern(DATE_FORMATS[counter]);
				fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
				log.info("PARSING " + listing[i] + " with " + DATE_FORMATS[counter]);				
				d = fmt.parse(listing[i], new ParsePosition(0));
				log.info("d IS NULL " + String.valueOf(d == null));
				
			    } while(d == null && counter < 3);
				
			    stamp.setTime(d);
			    
			    int today_of_month = now.get(Calendar.DAY_OF_MONTH);
			    int today_month = now.get(Calendar.MONTH);
			    int today_year = now.get(Calendar.YEAR);
			    
			    if( counter == 0 ) {
				stamp.set(Calendar.HOUR_OF_DAY, 23);
				stamp.set(Calendar.MINUTE, 59);
			    }			    
			    else if( counter == 1 ) {
				int today_of_week = now.get(Calendar.DAY_OF_WEEK);
				int fax_day_of_week = stamp.get(Calendar.DAY_OF_WEEK);
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
				
				log.info("SETTING STAMP TO THIS MONTH");				
				stamp.set(Calendar.MONTH, today_month);
				if( month_delta < 1 ) {
				    today_of_month = stamp.getActualMaximum(Calendar.DAY_OF_MONTH) + month_delta;  //month_delta is 0 or negative so add it to max number of days for the month
				}
				else {
				    today_of_month = month_delta;
				}
				stamp.set(Calendar.DAY_OF_MONTH, today_of_month);
				stamp.set(Calendar.YEAR, today_year);												
				
			    }
			    else if( counter == 2 ) {
				log.info("SETTING STAMP TO TODAY");
				stamp.set(Calendar.DAY_OF_MONTH, today_of_month);
				stamp.set(Calendar.MONTH, today_month);
				stamp.set(Calendar.YEAR, today_year);				
			    }
			    			    
			    local.setTime(stamp.getTime());
			    outgoing.setStamp(local.getTime());
			    
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
