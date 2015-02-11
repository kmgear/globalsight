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

package com.globalsight.everest.edit;

import com.globalsight.everest.tuv.Tuv;
import com.globalsight.log.GlobalSightCategory;
import com.globalsight.util.gxml.GxmlElement;
import com.globalsight.util.gxml.GxmlNames;
import com.sun.org.apache.bcel.internal.generic.RETURN;

import java.util.Vector;

/**
 * A set of helper methods used by the online and offline editors to
 * determine when a segment should be presented as protected.
 */
public class SegmentProtectionManager
{
    static private final GlobalSightCategory CATEGORY =
        (GlobalSightCategory)GlobalSightCategory.getLogger(
            SegmentProtectionManager.class);

    /** This class can not be instantiated. */
    private SegmentProtectionManager()
    {
    }

    /**
     * Determines if a tuvs state is a protected state.
     * @deprecated use the com.globalsight.edit.EditHelper class instead
     */
    static public boolean isTuvInProtectedState(Tuv p_tuv)
    {
        // Revised: 10-17-01 bb
        // We now protect all exact matches instead of just
        // LeverageGroupExactMatches.

        // return p_tuv.isLeverageGroupExactMatchLocalized();
        return p_tuv.isExactMatchLocalized();
    }

    /**
     * <p>Determines if a subflow element is to be excluded from
     * translation because of its item type.</p>
     *
     * @param p_element a GxmlElement that represents a translatable,
     * localizable, or SUB.
     * @param p_tuType the item type stored on TU level in case the
     * type was not specified for the GxmlElement.
     */
    static public boolean isTuvExcluded(GxmlElement p_element,
        String p_tuType, Vector p_excludedItemTypes)
    {
        String type = p_element.getAttribute(GxmlNames.SUB_TYPE);

        if (type == null)
        {
            type = p_tuType;
        }

        if (p_excludedItemTypes != null)
        {
            for (int i = 0; i < p_excludedItemTypes.size(); i++)
            {
                if (((String)p_excludedItemTypes.get(i)).equals(type))
                {
                    return true;
                }
            }
        }

        return false;
    }
    
    static public boolean isPreserveWhiteSpace(GxmlElement p_element)
    {
        String preserveWS = p_element.getAttribute(GxmlNames.SEGMENT_PRESERVEWS);
        return "yes".equals(preserveWS);
    }
    
    static public String handlePreserveWhiteSpace(GxmlElement p_element, CharSequence segment,
            String dir, String cssclass)
    {
        boolean isPreserve = isPreserveWhiteSpace(p_element);
        StringBuffer sb = new StringBuffer();
        
        if (isPreserve)
        {
            sb.append("<pre style='display: inline' ");

            if (dir != null)
            {
                sb.append(dir);
            }

            if (cssclass != null)
            {
                sb.append(" class='").append(cssclass).append("'");
            }
            
            sb.append(">");
        }
        sb.append(segment);
        if (isPreserve)
        {
            sb.append("</pre>");
        }
        
        return sb.toString();
    }
}
