package org.service;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;

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
	
	JSONUtils.getMorpherRegistry().registerMorpher( new EnumMorpher( FaxJob.STATUS.class ) );
	JSONObject jsonObject = JSONObject.fromObject(faxJobUpdated);
	
	return Response.ok(jsonObject.toString(), MediaType.APPLICATION_JSON).build();
	
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
	JSONUtils.getMorpherRegistry().registerMorpher( new EnumMorpher( FaxJob.STATUS.class ) );

	JSONArray jsonArray = JSONArray.fromObject(faxesList);	
			
	return Response.ok(jsonArray.toString(), MediaType.APPLICATION_JSON).build();
	
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
    	    
    	    JSONUtils.getMorpherRegistry().registerMorpher( new EnumMorpher( FaxJob.STATUS.class ) );
    	    JSONObject jsonObject = JSONObject.fromObject(faxList.get(0));
    	    return Response.ok(jsonObject.toString(), MediaType.APPLICATION_JSON).build();	        	            	   
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
