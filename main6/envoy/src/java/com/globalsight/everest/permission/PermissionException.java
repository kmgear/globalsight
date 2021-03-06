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
package com.globalsight.everest.permission;
import com.globalsight.util.GeneralException;

/**
* Basic exception class for the Permission package
*/
public class PermissionException extends GeneralException
{
    private static final String PROPERTY_FILE_NAME = "PermissionException";

    /*
     * Error messages
     */

    /**
     * Create a PermissionException with the specified message.
     *
     * @param p_messageKey The key to the message located in the property file.
     * @param p_messageArguments An array of arguments needed for the
     * message.  This can be null.
     * @param p_originalException The original exception that this one
     * is wrapping.  This can be null.
     */
    public PermissionException(String p_messageKey, String[] p_messageArguments, Exception p_originalException)
    {
        super(p_messageKey, p_messageArguments, p_originalException, PROPERTY_FILE_NAME);
    }

    /**
     * Create a ProjectHandlerException with the specified message.
     * @p_message The message.
     */
    public PermissionException(String p_message)
    {
        super(p_message);
    }

    /**
     * Creates a PermissionException
     * @see GeneralException#GeneralException(int, int, int, String)
     * @param p_message error message.
     * @param p_originalException original exception.
     *
     * @deprecated It doesn't take a raw message any more
     */
    public PermissionException(String p_message, Exception p_originalException)
    {
        super(p_message, p_originalException);
    }

    /** Just wraps an original exception */
    public PermissionException(Exception p_originalException)
    {
        super("",p_originalException);
    }
}

