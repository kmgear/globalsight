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
package com.globalsight.ling.tm;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.globalsight.ling.tm2.TmUtil;
import com.globalsight.ling.tm2.leverage.ModifyDateComparable;
import com.globalsight.ling.tm2.leverage.SidComparable;

/**
 * This class is used as return value from
 * com.globalsight.ling.tm.LeverageMatchLingManager#getExactMatches(List, long).
 * It holds a segment string to copy into a Tuv and a match type.
 */
public class LeverageSegment implements Serializable, SidComparable,
        ModifyDateComparable
{
    private static final long serialVersionUID = -3231007266035477757L;
    private String m_segment = null;
    private String m_matchType = null;
    private Timestamp modifyDate = null;
    private int tmIndex;
    private String orgSid = null;
    private String sid = null;
    private long tmId;
    private long matchedTuvId;

    public LeverageSegment()
    {
    }

    public LeverageSegment(String p_segment, String p_matchType,
            Date p_modifyDate, int tmIndex, String p_sid, long p_matchedTuvId,
            long p_tmId)
    {
        m_segment = p_segment;
        m_matchType = p_matchType;
        this.tmIndex = tmIndex;
        this.matchedTuvId = p_matchedTuvId;
        this.tmId = p_tmId;
        if (p_sid != null) {
            this.sid = p_sid;
        }
        if (p_modifyDate != null) {
            this.modifyDate = new Timestamp(p_modifyDate.getTime());
        }
    }

    public String getSid()
    {
        if (sid == null) {
            sid = TmUtil.getSidForTuv(tmId, matchedTuvId);
        }
        return sid;
    }

    public void setSid(String sid)
    {
        this.sid = sid;
    }

    public int getTmIndex()
    {
        return tmIndex;
    }

    public void setTmIndex(int tmIndex)
    {
        this.tmIndex = tmIndex;
    }

    public String getSegment()
    {
        return m_segment;
    }

    public void setSegment(String p_segment)
    {
        m_segment = p_segment;
    }

    public String getMatchType()
    {
        return m_matchType;
    }

    public void setMatchType(String p_matchType)
    {
        m_matchType = p_matchType;
    }

    public Timestamp getModifyDate()
    {
        if (modifyDate == null)
        {
            Date modifyDate = TmUtil.getModifyDateForTuv(tmId, matchedTuvId);
            if (modifyDate != null)
            {
                this.modifyDate = new Timestamp(modifyDate.getTime());
            }
        }
        return modifyDate;
    }

    public void setModifyDate(Timestamp modifyDate)
    {
        this.modifyDate = modifyDate;
    }

    public long getMatchedTuvId()
    {
        return matchedTuvId;
    }

    public void setMatchedTuvId(long matchedTuvId)
    {
        this.matchedTuvId = matchedTuvId;
    }

    @Override
    public String getOrgSid(long companyId)
    {
        return orgSid;
    }

    public void setOrgSid(String orgSid)
    {
        this.orgSid = orgSid;
    }
}
