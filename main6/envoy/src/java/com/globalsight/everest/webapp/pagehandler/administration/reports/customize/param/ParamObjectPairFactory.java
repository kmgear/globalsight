/**
 *  Copyright 2009 Welocalize, Inc. 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */
package com.globalsight.everest.webapp.pagehandler.administration.reports.customize.param;

public class ParamObjectPairFactory {

    public static ParamObjectPair getInstance(Param p_param)
    {
        String completeName = p_param.getCompletedName();
        if (completeName.equals(Param.JOB_ID))
        {
            return new ParamJobIdPair(p_param);
        }
        else if (completeName.equals(Param.JOB_DETAIL))
        {
            return new ParamJobDetailPair(p_param);
        }
        else if (completeName.equals(Param.STATUS))
        {
            return new ParamStatusPair(p_param);
        }
        else if (completeName.equals(Param.TM_MATCHES))
        {
            return new ParamTmMatchesPair(p_param);
        }
        else if (completeName.equals(Param.TRADOS_MATCHES))
        {
            return new ParamTradosMatchesPair(p_param);
        }
        else 
        {
            throw new RuntimeException("For " + completeName + "Not supported yet");
        }
    }
}
