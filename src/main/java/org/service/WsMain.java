package org.service;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.common.model.FaxJob;
import org.util.MiscUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WsMain {
    
    @GET
    @Produces("application/json")
    @Path("/{jobId}")
    public Response getFaxStatus(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("jobId") Long jobId) {
	
	FaxService faxService = FaxServiceFactory.create();
	
	FaxJob faxJob = new FaxJob();
	faxJob.setUser(user);
	faxJob.setPassword(passwd);
	faxJob.setJobId(jobId);
	
	FaxJob faxJobUpdated = faxService.getFaxStatus(faxJob);
	
	if( faxJobUpdated == null ) {
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}

	ObjectMapper mapper = new ObjectMapper();
	
	try {
	    String jsonObject = mapper.writeValueAsString(faxJobUpdated);
	
	    return Response.ok(jsonObject, MediaType.APPLICATION_JSON).build();
	}
	catch( IOException e ) {
	    MiscUtils.getLogger().error("ERROR CREATING JSON",e);
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}
	
    }
    
    
    @GET
    @Produces("application/json")
    @Path("/fax/{faxline}")
    public Response getFaxes(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("faxline") String faxline) {
	FaxService faxService = FaxServiceFactory.create();
	FaxJob faxJob = new FaxJob();
	faxJob.setUser(user);
	faxJob.setPassword(passwd);
	faxJob.setFax_line(faxline);	
	
	List<FaxJob> faxesList =  faxService.getFaxes(faxJob);
	
	MiscUtils.getLogger().info("RETURNING " + faxesList.size() + " faxes");
	ObjectMapper mapper = new ObjectMapper();
	
	String jsonArray;
	try {
	    jsonArray = mapper.writeValueAsString(faxesList);
	
	} catch (IOException e) {
	    MiscUtils.getLogger().error("ERROR CREATING JSON",e);
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}
			
	return Response.ok(jsonArray, MediaType.APPLICATION_JSON).build();
	
    }
    
    
    @GET
    @Produces("application/json")
    @Path("/fax/{faxline}/{filename}")
    public Response getFaxes(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("faxline") String faxline, @PathParam("filename") String filename) {
		
    	FaxService faxService = FaxServiceFactory.create();
    	FaxJob faxJob = new FaxJob();
    	
    	faxJob.setUser(user);
    	faxJob.setPassword(passwd);
    	faxJob.setFax_line(faxline);
    	faxJob.setFile_name(filename);
    	
    	List<FaxJob> faxList = faxService.getFaxes(faxJob);
    	
    	MiscUtils.getLogger().info("FILE IN faxList " + faxList.size());
    	
    	if( faxList.size() == 1 ) {
    	    
    	    ObjectMapper mapper = new ObjectMapper();
    	    
    	    String jsonObject;
	    try {
		jsonObject = mapper.writeValueAsString(faxList.get(0));
	    } catch (IOException e) {
		MiscUtils.getLogger().error(e);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	    }
    	    
    	    return Response.ok(jsonObject, MediaType.APPLICATION_JSON).build();	        	            	   
    	}
    	else {
    	    return Response.status(Response.Status.NOT_FOUND).build();
    	}
	
    }
    
    @DELETE
    @Path("/fax/{faxline}/{filename}")
    public Response deleteFax(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("faxline") String faxline, @PathParam("filename") String filename) {
	
	FaxService faxService = FaxServiceFactory.create();
	FaxJob faxJob = new FaxJob();
	faxJob.setUser(user);
	faxJob.setPassword(passwd);
	faxJob.setFax_line(faxline);
	faxJob.setFile_name(filename);
	
	//if successful return success else error
	if( faxService.deleteFax(faxJob) ) {
	    return Response.status(Response.Status.NO_CONTENT).build();
	}
	else {
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}
    }
    
    @POST        
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/fax/send/{user}")
    public Response sendFax(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("user") String faxUser, FaxJob faxJob) {
	
	faxJob.setUser(user);
	faxJob.setPassword(passwd);
	
	FaxService faxService = FaxServiceFactory.create();
	
	FaxJob faxJobSent = faxService.sendFax(faxJob);
	
	if( faxJobSent == null ) {
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}
	
	return Response.ok(faxJobSent, MediaType.APPLICATION_XML).build();
	
    }
    
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fax/{jobId}")
    public Response cancelFax(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("jobId") String jobId ) {
	
	FaxService faxService = FaxServiceFactory.create();
	
	if( faxService.cancelFax(user, passwd, jobId) ) {
	    String json = "{success:true}";
	    return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}
	
	return Response.status(Response.Status.CONFLICT).build();
	
    }

}
