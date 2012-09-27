// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.SuccessResponse;
import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Deletes a specified domain", responseObject=SuccessResponse.class)
public class DeleteDomainCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteDomainCmd.class.getName());
    private static final String s_name = "deletedomainresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="ID of domain to delete")
    private Long id;

    @Parameter(name=ApiConstants.CLEANUP, type=CommandType.BOOLEAN, description="true if all domain resources (child domains, accounts) have to be cleaned up, false otherwise")
    private Boolean cleanup;

    @Parameter(name=ApiConstants.IS_PROPAGATE, type=CommandType.BOOLEAN, description="True if command is sent from another Region")
    private Boolean isPropagate;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getCleanup() {
        return cleanup;
    }

	public Boolean getIsPropagate() {
		return isPropagate;
	}
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Domain domain = _entityMgr.findById(Domain.class, getId());
        if (domain != null) {
            return domain.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DOMAIN_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "deleting domain: " + getId();
    }
    
    @Override
    public void execute(){
        UserContext.current().setEventDetails("Domain Id: "+getId());
        boolean isPopagate = (getIsPropagate() != null ) ? getIsPropagate() : false; 
        boolean result = false;
        if(isPopagate){
        	result = _domainService.deleteDomain(id, cleanup);
        } else {
        	result = _regionService.deleteDomain(id, cleanup);
        }
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete domain");
        }
    }
}
