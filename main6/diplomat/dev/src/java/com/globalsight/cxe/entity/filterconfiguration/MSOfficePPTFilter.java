package com.globalsight.cxe.entity.filterconfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.globalsight.persistence.hibernate.HibernateUtil;

public class MSOfficePPTFilter implements Filter
{
    private long id;
    private String filterName;
    private String filterDescription;
    private long secondFilterId = -2;
    private String secondFilterTableName = null;
    private long companyId;
    private boolean extractAlt = false;

    @SuppressWarnings("unchecked")
    public ArrayList<Filter> getFilters(long companyId)
    {
        ArrayList<Filter> filters = null;
        filters = new ArrayList<Filter>();
        String hql = "from MSOfficePPTFilter jp where jp.companyId="
                + companyId;
        try{
            filters = (ArrayList<Filter>) HibernateUtil.search(hql);
        }
        catch( Exception e) {
            e.printStackTrace();
        }
        return filters;
    }

    public boolean checkExists(String filterName, long companyId)
    {
        String hql = "from MSOfficePPTFilter jp " + 
                     "where jp.filterName =:filterName " + 
                     "and jp.companyId=:companyId";
        Map map = new HashMap();
        map.put("filterName", filterName);
        map.put("companyId", companyId);
        return HibernateUtil.search(hql, map).size() > 0;
    }

    public String getFilterTableName()
    {
        return FilterConstants.MSOFFICEPPT_TABLENAME;
    }

    public String toJSON(long companyId)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"filterTableName\":").append(
                "\"" + FilterConstants.MSOFFICEPPT_TABLENAME + "\"").append(
                ",");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"filterName\":").append("\"").append(
                FilterHelper.escape(filterName)).append("\"").append(",");
        sb.append("\"filterDescription\":").append("\"").append(
                FilterHelper.escape(filterDescription)).append("\"")
                .append(",");
        sb.append("\"companyId\":").append(companyId).append(",");
        sb.append("\"extractAlt\":").append(extractAlt).append(",");
        sb.append("\"secondFilterId\":").append(secondFilterId).append(",");
        sb.append("\"secondFilterTableName\":").append("\"").append(
                FilterHelper.escape(secondFilterTableName)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    public long getId()
    {
        return id;
    }
    
    public void setId(long id)
    {
        this.id = id;
    }

    public String getFilterName()
    {
        return filterName;
    }

    public void setFilterName(String filterName)
    {
        this.filterName = filterName;
    }

    public String getFilterDescription()
    {
        return filterDescription;
    }

    public void setFilterDescription(String filterDescription)
    {
        this.filterDescription = filterDescription;
    }

    public long getCompanyId()
    {
        return companyId;
    }

    public void setCompanyId(long companyId)
    {
        this.companyId = companyId;
    }
    
    public void setSecondFilterId(long secondFilterId)
    {
        this.secondFilterId = secondFilterId;
    }
    
    public long getSecondFilterId() 
    {
        return this.secondFilterId;
    }
    
    public void setSecondFilterTableName(String secondFilterTableName)
    {
        this.secondFilterTableName = secondFilterTableName;
    }
    
    public String getSecondFilterTableName()
    {
        return this.secondFilterTableName;
    }
    
    public boolean getExtractAlt()
    {
        return extractAlt;
    }

    public void setExtractAlt(boolean extractAlt)
    {
        this.extractAlt = extractAlt;
    }
}
