package org.service;

import java.util.List;

import org.common.model.FaxJob;

public interface FaxService {
    
    public FaxJob getFaxStatus(FaxJob faxJob);
    
    public FaxJob sendFax(FaxJob faxJob);
	
    public List<FaxJob> getFaxes(FaxJob faxJob);
    
    public boolean deleteFax(FaxJob fax);
    
    public boolean cancelFax(String user, String passwd, String jobId);
}
