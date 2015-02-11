package com.globalsight.ling.tm2.segmenttm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.globalsight.everest.corpus.CorpusManager;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.tm.Tm;
import com.globalsight.everest.tm.TmRemover;
import com.globalsight.ling.tm2.lucene.LuceneIndexWriter;
import com.globalsight.ling.tm2.persistence.DbUtil;
import com.globalsight.log.GlobalSightCategory;

/**
 * This contains a lot of code that used to live in 
 * com.globalsight.everest.tm.TmRemover.  It handles bulk removal
 * of TM2 segment tms.
 */
public class TmRemoveHelper {
    static private final GlobalSightCategory c_logger = 
        (GlobalSightCategory) GlobalSightCategory.getLogger(TmRemover.class);

    static class Query {
        static final String REMOVE_TU_T = "delete from project_tm_tu_t where tm_id = ?";
    
        static final String REMOVE_TU_L = "delete from project_tm_tu_l where tm_id = ?";
    
        static final String REMOVE_TUV_T = "delete from project_tm_tuv_t where exists "
                + "(select * from project_tm_tu_t where project_tm_tu_t.id = project_tm_tuv_t.tu_id "
                + "and project_tm_tu_t.tm_id = ?)";
        
        static final String REMOVE_TUV_T_BY_LANGUAGE = "delete from project_tm_tuv_t where exists "
               + "(select * from project_tm_tu_t where project_tm_tu_t.id = project_tm_tuv_t.tu_id "
               + " and  project_tm_tu_t.tm_id= ? "
               + "and project_tm_tuv_t.LOCALE_ID = ?)";
    
        static final String REMOVE_TUV_L = "delete from project_tm_tuv_l where exists "
                + "(select * from project_tm_tu_l where project_tm_tu_l.id = project_tm_tuv_l.tu_id "
                + "and project_tm_tu_l.tm_id = ?)";
        
        static final String REMOVE_TUV_L_BY_LANGUAGE = "delete from project_tm_tuv_l where exists "
               + "(select * from project_tm_tu_l where project_tm_tu_l.id = project_tm_tuv_l.tu_id "
               + " and project_tm_tu_l.tm_id = ? "
               + "and  project_tm_tuv_l.LOCALE_ID = ? )";

    }

    static final String GET_CUV_IDS = "select distinct cm.cuv_id from corpus_map cm, "
        + "project_tm_tu_t tu where cm.project_tu_id = tu.id and tu.tm_id = ?";
    
    static final String GET_CUV_IDS_T_BY_LANGUAGE = 
        "select distinct cm.cuv_id from corpus_map cm, project_tm_tu_t tu, "
        + "project_tm_tuv_t tuv where cm.project_tu_id = tu.id and tuv.LOCALE_ID = ? "
        + " and tuv.tu_id = tu.id and tu.tm_id = ?";
    
    static final String GET_CUV_IDS_L_BY_LANGUAGE = 
        "select distinct cm.cuv_id from corpus_map cm, project_tm_tu_l tu, "
        + "project_tm_tuv_l tuv where cm.project_tu_id = tu.id and tuv.LOCALE_ID = ? "
        + " and tuv.tu_id = tu.id and tu.tm_id = ?";
    
    /**
     * Remove corpus data associated with a given TM and optionally locale.
     * This is meant to work across Segment TM implementations.
     * 
     * @param conn JDBC connection
     * @param tmId tm id
     * @param localeId locale id, or null to delete for all locales
     * @throws Exception
     *
     */
    public static void removeCorpus(Connection conn, long tmId, Long localeId) 
                throws Exception {
        List<Long> cuvIdList = new ArrayList<Long>();

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try
        {
            if (localeId != null) {
                stmt = conn.prepareStatement(GET_CUV_IDS_T_BY_LANGUAGE);
                stmt.setLong(1, localeId);
                stmt.setLong(2, tmId);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    cuvIdList.add(rs.getLong(1));
                }
                
                stmt = conn.prepareStatement(GET_CUV_IDS_L_BY_LANGUAGE);
                stmt.setLong(1, localeId);
                stmt.setLong(2, tmId);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    cuvIdList.add(rs.getLong(1));
                }
            }
            else {
                stmt = conn.prepareStatement(GET_CUV_IDS);
                stmt.setLong(1, tmId); 
                rs = stmt.executeQuery();

                while (rs.next()) {
                    cuvIdList.add(rs.getLong(1));
                }
            }
        }
        finally
        {
            DbUtil.silentClose(stmt);
            DbUtil.silentClose(rs);
        }

        c_logger.info("Deleting corpus docs with these cuv_id: " + cuvIdList);

        CorpusManager corpusManager = ServerProxy.getCorpusManager();
        for (Iterator it = cuvIdList.iterator(); it.hasNext();)
        {
            corpusManager.removeCorpusDoc((Long) it.next());
        }
    }

    public static void removeTm(Tm tm) throws Exception {
        String tmName = tm.getName();
        ServerProxy.getProjectHandler().removeProjectTm(tm.getId());
        c_logger.info("Removed Tm: " + tmName);
    }

    static void removeDataByLocale(Connection connection, long tmId, 
                long localeId, String p_sqlString) throws Exception
    {
        PreparedStatement ps = null;

        try
        {
            ps = connection.prepareStatement(p_sqlString);
            ps.setLong(1, tmId);
            ps.setLong(2, localeId);
            
            int num = ps.executeUpdate();

            c_logger.info("Removed " + num + " rows from a Tm table.");
        }
        finally
        {
            if (ps != null)
            {
                try
                {
                    ps.close();
                }
                catch (SQLException ignore)
                {
                }
            }
        }
    }
    static void removeData(Connection connection, long tmId, 
            String p_sqlString) throws Exception
    {
        PreparedStatement ps = null;
    
        try
        {
            ps = connection.prepareStatement(p_sqlString);
            ps.setLong(1, tmId);
            int num = ps.executeUpdate();
    
            c_logger.info("Removed " + num + " rows from a Tm table.");
        }
        finally
        {
            if (ps != null)
            {
                try
                {
                    ps.close();
                }
                catch (SQLException ignore)
                {
                }
            }
        }
    }
    
    static void removeIndex(long tmId) throws Exception
    {
        LuceneIndexWriter.removeTm(tmId);
        c_logger.info("Removed Tm index.");
    }


}
