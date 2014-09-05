package org.common.dao;

import org.common.model.FaxJob;
import org.springframework.stereotype.Repository;

@Repository
public class FaxJobDao extends AbstractDao<FaxJob> {
    
    public FaxJobDao() {
	super(FaxJob.class);
    }

}
