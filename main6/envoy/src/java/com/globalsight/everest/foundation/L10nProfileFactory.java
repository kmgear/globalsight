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
package com.globalsight.everest.foundation;

/*
 * Copyright (c) 2000 GlobalSight Corporation. All rights reserved.
 *
 * THIS DOCUMENT CONTAINS TRADE SECRET DATA WHICH IS THE PROPERTY OF 
 * GLOBALSIGHT CORPORATION. THIS DOCUMENT IS SUBMITTED TO RECIPIENT
 * IN CONFIDENCE. INFORMATION CONTAINED HEREIN MAY NOT BE USED, COPIED
 * OR DISCLOSED IN WHOLE OR IN PART EXCEPT AS PERMITTED BY WRITTEN
 * AGREEMENT SIGNED BY AN OFFICER OF GLOBALSIGHT CORPORATION.
 *
 * THIS MATERIAL IS ALSO COPYRIGHTED AS AN UNPUBLISHED WORK UNDER
 * SECTIONS 104 AND 408 OF TITLE 17 OF THE UNITED STATES CODE.
 * UNAUTHORIZED USE, COPYING OR OTHER REPRODUCTION IS PROHIBITED
 * BY LAW.
 */

// Core Java classes
import java.sql.Timestamp;

import com.globalsight.everest.company.CompanyThreadLocal;
import com.globalsight.everest.projecthandler.WorkflowTemplateInfo;
import com.globalsight.util.GlobalSightLocale;
/**
 * This class provides factory methods to create a localization profile. 
 * 
 */

public class L10nProfileFactory
{
    /**
     * Construct a new instance of BasicL10nProfile given an instance of
     * a class that implements the L10nProfile interface.
     *
     * @param p_l10nProfile An instance of a class that implements the
     *        l10nProfile interface.
     * @return A new instance of BasicL10nProfile that has copies of
     *         attributes from the provided instance of the class that
     *         implements L10nProfile.
     */
    public static BasicL10nProfile makeBasicL10nProfile(L10nProfile p_l10nProfile)
    {
        BasicL10nProfile basicProfile = new BasicL10nProfile(p_l10nProfile.getName());
        basicProfile.setPriority(p_l10nProfile.getPriority());
        basicProfile.setAutomaticDispatch(p_l10nProfile.dispatchIsAutomatic());
        basicProfile.setProjectId(p_l10nProfile.getProjectId());
        if (p_l10nProfile.getProject() != null)
        {
            basicProfile.setProject(p_l10nProfile.getProject());
        }
        basicProfile.setTMChoice(p_l10nProfile.getTMChoice());
        basicProfile.setDescription(p_l10nProfile.getDescription());
        basicProfile.setCompanyId(CompanyThreadLocal.getInstance().getValue());
        basicProfile.setSourceLocale(p_l10nProfile.getSourceLocale());
        basicProfile.setTimestamp(new Timestamp(System.currentTimeMillis()));
        
        // Copy target locales and associated workflow template ids
        GlobalSightLocale[] targetLocales = p_l10nProfile.getTargetLocales();
        for (int i = 0; i < targetLocales.length; i++)
        {
            WorkflowTemplateInfo wf = p_l10nProfile.getWorkflowTemplateInfo(targetLocales[i]);

            // clones the workflow and tasks - with cleared out ids
            WorkflowTemplateInfo newWf = wf.cloneForInsert();
            basicProfile.addWorkflowTemplateInfo(newWf);
        }
        return basicProfile;
    }
    public static BasicL10nProfile makeDuplicateL10nProfile(L10nProfile p_l10nProfile,
                                                            GlobalSightLocale p_gsl)
    {
        BasicL10nProfile duplicateProfile = new BasicL10nProfile();
        duplicateProfile.setName(p_l10nProfile.getName());
        duplicateProfile.setPriority(p_l10nProfile.getPriority());
        duplicateProfile.setAutomaticDispatch(p_l10nProfile.dispatchIsAutomatic());
        duplicateProfile.setProjectId(p_l10nProfile.getProjectId());
        if (p_l10nProfile.getProject() != null)
        {
            duplicateProfile.setProject(p_l10nProfile.getProject());
        }
        duplicateProfile.setDescription(p_l10nProfile.getDescription());
        duplicateProfile.setCompanyId(CompanyThreadLocal.getInstance().getValue());
        duplicateProfile.setSourceLocale(p_gsl);
        duplicateProfile.setTMChoice(p_l10nProfile.getTMChoice());
        duplicateProfile.setDispatchCriteria(p_l10nProfile.getDispatchCriteria());
        duplicateProfile.setExactMatchEditing(p_l10nProfile.isExactMatchEditing());
        duplicateProfile.setRunScriptAtJobCreation(p_l10nProfile.runScriptAtJobCreation());
        duplicateProfile.setJobCreationScriptName(p_l10nProfile.getNameOfJobCreationScript());
        return duplicateProfile;
    }
}
