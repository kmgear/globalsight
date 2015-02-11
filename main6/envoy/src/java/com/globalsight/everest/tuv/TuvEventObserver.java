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

package com.globalsight.everest.tuv;

//
// globalsight imports
//
import com.globalsight.everest.tuv.TuvException;

//
// Java imports
//
import java.util.Collection;

//
//
import java.rmi.RemoteException;

/**
 * This class represents an observer of events that affect Tuv.
 * The callers notify the observer of an event that could
 * have an affect on the state of the Tuv.
 */
public interface TuvEventObserver
{
    // The name bound to the remote object.
    public static final String SERVICE_NAME = "TuvEventObserver";

    /**
     * Notification that the target locale page of
     * Tuvs has been exported.
     * @param p_targetTuvs target locale page Tuvs.
     * @throws TuvException when an error occurs.
     */
    public void notifyPageExportedEvent(Collection p_targetTuvs)
            throws TuvException, RemoteException;


    /**
     * Updates the state of target page TUVs that are LOCALIZED to COMPLETED
     * otherwise leaves the state alone. Should be called after an export
     * for update of a source page.
     * 
     * @param p_targetTuvs
     *               collection of target TUVs
     * @exception TuvException
     * @exception RemoteException
     */
    public void notifyPageExportedForUpdateEvent(Collection p_targetTuvs)
            throws TuvException, RemoteException;


    /**
     * Notification that the job of source locale page
     * Tuvs has been exported.
     * @param p_sourceTuvs source locale page Tuvs.
     * @throws TuvException when an error occurs.
     */
    public void notifyJobExportedEvent(Collection p_sourceTuvs)
            throws TuvException, RemoteException;

    /**
     * Notification that the workflow task completed for
     * this target locale page of Tuvs.
     * @param p_targetTuvs target locale page Tuvs.
     * @param p_taskId task identifier that completed.
     * @throws TuvException when an error occurs.
     */
    public void notifyTaskCompleteEvent(Collection p_targetTuvs,
            long p_taskId)
            throws TuvException, RemoteException;

    /**
     * Notification that the workflow last task completed for
     * this target locale page of Tuvs.
     * @param p_targetTuvs target locale page Tuvs.
     * @param p_taskId task identifier that completed.
     * @throws TuvException when an error occurs.
     */
    public void notifyLastTaskCompleteEvent(Collection p_targetTuvs,
            long p_taskId)
            throws TuvException, RemoteException;


    /**
     * Notification that the workflow of target locale page
     * Tuvs has been cancelled.
     * @param p_targetTuvs target locale page Tuvs.
     * @throws TuvException when an error occurs.
     */
    public void notifyWorkflowCancelEvent(Collection p_targetTuvs)
            throws TuvException, RemoteException;

    /**
     * Notification that all the workflows of target locale page
     * Tuvs have been cancelled.
     * @param p_targetTuvs target locale page Tuvs.
     * @throws TuvException when an error occurs.
     */
    public void notifyAllWorkflowsCancelEvent(Collection p_targetTuvs)
            throws TuvException, RemoteException;
}
