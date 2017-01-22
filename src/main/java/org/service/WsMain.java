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

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
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

	Logger logger =  MiscUtils.getLogger();

	@GET
	@Produces("application/json")
	@Path("/{jobId}")
	public Response getFaxStatus(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("jobId") Long jobId) {

		FaxService faxService = FaxServiceFactory.create();

		FaxJob faxJob = new FaxJob();
		faxJob.setUser(user);
		faxJob.setPassword(passwd);
		faxJob.setJobId(jobId);
		String jsonObject = null;
		FaxJob faxJobUpdated = null;

		try {
			faxJobUpdated = faxService.getFaxStatus(faxJob);
			ObjectMapper mapper = new ObjectMapper();
			jsonObject = mapper.writeValueAsString(faxJobUpdated);			
		} catch (IOException e1) {
			logger.error("Error while retreiving fax status.", e1);
			faxJobUpdated = null;
		} finally {
			if( faxJobUpdated == null ) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
		}

		return Response.ok(jsonObject, MediaType.APPLICATION_JSON).build();

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
		String jsonArray = null;

		try {

			List<FaxJob> faxesList =  faxService.getFaxes(faxJob);			
			MiscUtils.getLogger().info("RETURNING " + faxesList.size() + " faxes");			
			ObjectMapper mapper = new ObjectMapper();			
			jsonArray = mapper.writeValueAsString(faxesList);

		} catch (IOException e) {
			logger.error("Error while retrieving fax list from server", e);
			jsonArray = null;
		} finally {
			if( jsonArray == null ) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
		}

		return Response.ok(jsonArray, MediaType.APPLICATION_JSON).build();

	}


	@GET
	@Produces("application/json")
	@Path("/fax/{faxline}/{filename}")
	public Response getFaxes(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("faxline") String faxline, @PathParam("filename") String filename) {

		FaxService faxService = FaxServiceFactory.create();
		FaxJob faxJob = new FaxJob();	    
		String jsonObject;

		faxJob.setUser(user);
		faxJob.setPassword(passwd);
		faxJob.setFax_line(faxline);
		faxJob.setFile_name(filename);

		FaxJob fax = null;

		try {
			fax = faxService.getFax(faxJob);
		} catch (IOException e1) {
			logger.error("Error while retrieving fax list from server", e1);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		} 

		if( fax != null ) {

			logger.info("Found fax file " + filename);

			ObjectMapper mapper = new ObjectMapper();

			try {
				jsonObject = mapper.writeValueAsString(fax);
			} catch (IOException e) {
				logger.error("Error while creating JSON response", e);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}

			return Response.ok(jsonObject, MediaType.APPLICATION_JSON).build();	 

		} else {
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
		Boolean deleted = Boolean.FALSE;
		//if successful return success else error
		try {

			deleted = faxService.deleteFax(faxJob);

		} catch (IOException e) {
			logger.error("Error while deleting fax", e);
			deleted = Boolean.FALSE;
		} finally {
			if( ! deleted ) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			} 
		}

		return Response.status(Response.Status.NO_CONTENT).build();

	}

	@POST        
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("/fax/send/{user}")
	public Response sendFax(@HeaderParam("user") String user, @HeaderParam("passwd") String passwd, @PathParam("user") String faxUser, FaxJob faxJob) {

		faxJob.setUser(user);
		faxJob.setPassword(passwd);

		FaxService faxService = FaxServiceFactory.create();	
		FaxJob faxJobSent = null;

		try {
			faxJobSent = faxService.sendFax(faxJob);
		} catch (IOException e) {
			logger.error("Failed to send fax ", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

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
		Boolean cancelled = Boolean.FALSE;
		String json = "{success:false}";
		
		try {
			cancelled = faxService.cancelFax(user, passwd, jobId);
		} catch (IOException e) {
			logger.error("Failed to cancel fax ", e);
			cancelled = Boolean.FALSE;
		} finally {
			if( ! cancelled ) {
				return Response.status(Response.Status.CONFLICT).build();
			} else {
				json = "{success:true}";
			}
		}

		return Response.ok(json, MediaType.APPLICATION_JSON).build();

	}

}
