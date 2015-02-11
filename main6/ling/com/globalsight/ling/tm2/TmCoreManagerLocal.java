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
package com.globalsight.ling.tm2;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.naming.NamingException;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.globalsight.diplomat.util.database.DBConnection;
import com.globalsight.everest.page.SourcePage;
import com.globalsight.everest.projecthandler.ProjectTM;
import com.globalsight.everest.projecthandler.ProjectHandler;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.tm.StatisticsInfo;
import com.globalsight.everest.tm.Tm;
import com.globalsight.ling.inprogresstm.DynamicLeverageResults;
import com.globalsight.ling.tm.LingManagerException;
import com.globalsight.ling.tm2.corpusinterface.TuvMappingHolder;
import com.globalsight.ling.tm2.indexer.Reindexer;
import com.globalsight.ling.tm2.leverage.LeverageDataCenter;
import com.globalsight.ling.tm2.leverage.LeverageMatchResults;
import com.globalsight.ling.tm2.leverage.LeverageMatches;
import com.globalsight.ling.tm2.leverage.LeverageOptions;
import com.globalsight.ling.tm2.leverage.Leverager;
import com.globalsight.ling.tm2.leverage.RemoteLeverager;
import com.globalsight.ling.tm2.persistence.DbUtil;
import com.globalsight.ling.tm2.persistence.LeverageMatchSaver;
import com.globalsight.ling.tm2.persistence.PageJobDataRetriever;
import com.globalsight.ling.tm2.persistence.SegmentQueryResult;
import com.globalsight.ling.tm2.persistence.error.BatchException;
import com.globalsight.ling.tm2.population.TmPopulator;
import com.globalsight.ling.tm2.segmenttm.Tm2Reindexer;
import com.globalsight.ling.tm2.segmenttm.Tm2SegmentTmInfo;
import com.globalsight.ling.tm2.segmenttm.TmRemoveHelper;
import com.globalsight.ling.tm2.segmenttm.TmConcordanceQuery.TMidTUid;
import com.globalsight.ling.tm3.integration.segmenttm.Tm3SegmentTmInfo;
import com.globalsight.log.GlobalSightCategory;
import com.globalsight.persistence.hibernate.HibernateUtil;
import com.globalsight.util.GlobalSightLocale;
import com.globalsight.util.progress.InterruptMonitor;
import com.globalsight.util.progress.ProgressReporter;

/**
 * Implementation of TmCoreManager
 */

public class TmCoreManagerLocal implements TmCoreManager
{
    // object to lock for synchronizing tm population process (due to deadlocks)
    private Boolean m_tmPopulationLock = new Boolean(true);

    private static final GlobalSightCategory c_logger = (GlobalSightCategory) GlobalSightCategory
            .getLogger(TmCoreManagerLocal.class);

    /**
     * Save source and target segments that belong to the page. Segments are
     * saved in Page Tm and Segment Tm.
     * 
     * @param p_page
     *            source page that has been exported.
     * @param p_options
     *            Tm options. It has information which Project TM segments
     *            should be saved and etc.
     * @return mappings of translation_unit_variant id and project_tm_tuv_t id
     *         of this page
     */
    public TuvMappingHolder populatePageForAllLocales(SourcePage p_page,
            LeverageOptions p_options) throws LingManagerException
    {
        TuvMappingHolder mappingHolder = null;
        Session session = TmUtil.getStableSession();
        try
        {
            TmPopulator tmPopulator = new TmPopulator(session);
            mappingHolder
                = tmPopulator.populatePageForAllLocales(p_page, p_options);
        }
        catch (LingManagerException le)
        {
            throw le;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
        return mappingHolder;
    }

    /**
     * Save source and a specified target segments that belong to the page.
     * Segments are saved in Page Tm and Segment Tm.
     * 
     * @param p_page
     *            source page that has been exported.
     * @param p_options
     *            Tm options. It has information which Project TM segments
     *            should be saved and etc.
     * @param p_locale
     *            target locale
     * @return mappings of translation_unit_variant id and project_tm_tuv_t id
     *         of this page
     */
    public TuvMappingHolder populatePageByLocale(SourcePage p_page,
            LeverageOptions p_options, GlobalSightLocale p_locale)
            throws LingManagerException
    {
        TuvMappingHolder mappingHolder = null;

        Session session = TmUtil.getStableSession();
        try
        {
            synchronized(m_tmPopulationLock)
            {
                TmPopulator tmPopulator = new TmPopulator(session);
                mappingHolder = tmPopulator.populatePageByLocale(
                    p_page, p_options, p_locale);
            }
        }
        catch (LingManagerException le)
        {
            throw le;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally {
            TmUtil.closeStableSession(session);
        }

        return mappingHolder;
    }

    /**
     * create LeverageDataCenter with original source segments from a source
     * page
     * 
     * @param p_sourcePage
     *            SourcePage
     * @param p_leverageOptions
     *            LeverageOptions
     * @return LeverageDataCenter object
     */
    public LeverageDataCenter createLeverageDataCenterForPage(
            SourcePage p_sourcePage, LeverageOptions p_leverageOptions)
            throws LingManagerException
    {
        Connection conn = null;
        LeverageDataCenter leverageDataCenter = null;
        PageJobDataRetriever pageJobDataRetriever = null;

        try
        {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            GlobalSightLocale sourceLocale = p_sourcePage
                    .getGlobalSightLocale();

            // prepare a repository of original segments (source
            // segments in translation_unit_variant)
            leverageDataCenter = new LeverageDataCenter(sourceLocale,
                    p_leverageOptions.getLeveragingLocales()
                            .getAllTargetLocales(), p_leverageOptions);

            // Get page data from translation_unit_variant and
            // translation_unit table
            pageJobDataRetriever = new PageJobDataRetriever(conn, p_sourcePage
                    .getId(), sourceLocale);
            SegmentQueryResult result = pageJobDataRetriever.queryForLeverage();

            BaseTmTu tu = null;
            while ((tu = result.getNextTu()) != null)
            {
                // Don't add a tuv that has an excluded type
                if (!p_leverageOptions.isExcluded(tu.getType()))
                {
                    BaseTmTuv tuv = tu.getFirstTuv(sourceLocale);
                    leverageDataCenter.addOriginalSourceTuv(tuv);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.setAutoCommit(true);
                    DbUtil.returnConnection(conn);
                }
                catch (Exception e)
                {
                    throw new LingManagerException(e);
                }
            }

            if (pageJobDataRetriever != null)
            {
                try
                {
                    pageJobDataRetriever.close();
                }
                catch (Exception e)
                {
                    throw new LingManagerException(e);
                }
            }
        }

        return leverageDataCenter;
    }

    /**
     * save leverage results stored in a LeverageDataCenter object to
     * leverage_match table.
     * 
     * @param p_leverageDataCenter
     *            LeverageDataCenter object
     * @param p_sourcePage
     *            SourcePage object
     */
    public void saveLeverageResults(LeverageDataCenter p_leverageDataCenter,
            SourcePage p_sourcePage) throws LingManagerException
    {
        Connection conn = null;

        try
        {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            c_logger.debug("save matches begin");

            // save matches to leverage_match table
            LeverageMatchSaver levMatchSaver = new LeverageMatchSaver(conn);
            levMatchSaver.saveMatchesToDb(p_sourcePage, p_leverageDataCenter);

            c_logger.debug("save matches end");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.setAutoCommit(true);
                    DbUtil.returnConnection(conn);
                }
                catch (Exception e)
                {
                    throw new LingManagerException(e);
                }
            }
        }
    }

    /**
     * Leverage a given page. It does: - leverage Page Tm - leverage Segment Tm
     * - apply leverage options for both leverage matches - save matches to the
     * database - returns a list of exact matched segments
     * 
     * @param p_sourcePage
     *            source page
     * @param p_leverageDataCenter
     *            LeverageDataCenter object
     */
    public void leveragePage(SourcePage p_sourcePage,
            LeverageDataCenter p_leverageDataCenter)
            throws LingManagerException
    {
        // default remote leveraging page from remote tm
        // if it is configured in tm profile
        this.leveragePage(p_sourcePage, p_leverageDataCenter, true);
    }

    
    /**
     * Leverage all the segments in a source page against a set
     * of TMs identified as part of the LDC/LeverageOptions.  Because
     * these TMs may not all have the same storage implementation
     * (legacy tm2 vs tm3), we need to split up the TMs into groups,
     * call each engine separately, then merge the results.  Ugly!
     * Luckily, this should only matter in the transient/upgrade 
     * case.  At some point, one would hope that everyone is on 
     * tm3 and we can dump the tm2 code entirely.  
     */
    public void leveragePage(SourcePage p_sourcePage,
            LeverageDataCenter p_leverageDataCenter, boolean p_leverageRemoteTm)
            throws LingManagerException
    {
        LeverageOptions leverageOptions
        	= p_leverageDataCenter.getLeverageOptions();
    
        // if number of matches returned option is less than 1, no
        // action will be taken.
        if(leverageOptions.getNumberOfMatchesReturned() < 1)
        {
            return;
        }
    
        // Split the tms up by implementation
        SortedTms sortedTms = sortTmsByImplementation(leverageOptions.getLeverageTms());
     
        Session session = TmUtil.getStableSession();
        
        try
        {
        	Leverager leverager = new Leverager(session);
        	// Leverage the tm2 and tm3 tms
        	leverager.leveragePage(p_sourcePage, p_leverageDataCenter,
        	                       sortedTms.tm2Tms, sortedTms.tm3Tms);
        	
        	// Leverage the remote TMs
            
            if (p_leverageRemoteTm && sortedTms.remoteTms.size() > 0) {
                new RemoteLeverager().remoteLeveragePage(p_sourcePage, sortedTms.remoteTms, 
                                                         p_leverageDataCenter);
            }
        }
        catch (LingManagerException le)
        {
            le.printStackTrace();
            throw le;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }

    /**
     * Saves segments to a Segment Tm. Use <code>p_mode</code> to overwrite
     * existing TUs, merge existing TUs with new TUs, or to discard new TUs if
     * they already exist in the TM.
     * 
     * @param p_tmId
     *            Tm id in which segments are saved
     * @param p_segments
     *            Collection of SegmentTmTu objects.
     * @param p_mode
     *            one of the "SYNC" constants, to overwrite existing TUs, merge
     *            existing TUs with new TUs, or to discard new TUs if they
     *            already exist in the TM.
     * @return TuvMappingHolder. m_tuvId and m_tuId values are arbitrary.
     * 
     * @throws LingManagerException
     */
    @Override
    public TuvMappingHolder saveToSegmentTm(
        Tm tm, Collection p_segments, int p_mode)
        throws LingManagerException
    {
        if (p_segments.size() == 0)
        {
            return new TuvMappingHolder();
        }

        TuvMappingHolder holder = null;        
        Session session = TmUtil.getStableSession();
        try
        {
            TmPopulator tmPopulator = new TmPopulator(session);
            holder = tmPopulator.saveSegmentToSegmentTm(
                p_segments, tm, p_mode);
        }
        catch (LingManagerException le)
        {
            throw le;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
        
        return holder;
    }
    
    /**
     * Saves a collection of segments to a segment TM.  A BatchException does
     * not mean that the save failed, only that some segments could not be
     * saved (and the BatchException has the details).
     */
    @Override
    public TuvMappingHolder saveToSegmentTm(
            Tm tm, Collection p_segments, int p_mode, String p_sourceTmName)
            throws LingManagerException, BatchException
    {
        if (p_segments.size() == 0)
        {
            return new TuvMappingHolder();
        }
        Collection<SegmentTmTu> goodSegments = new ArrayList<SegmentTmTu>();
        Collection<BatchException.TuvError> errs =
            new ArrayList<BatchException.TuvError>();
        for (Object _tu : p_segments) {
            SegmentTmTu tu = (SegmentTmTu) _tu;
            boolean badTu = false;
            for (BaseTmTuv _tuv : tu.getTuvs()) {
                SegmentTmTuv tuv = (SegmentTmTuv) _tuv;
                char[] chars = tuv.getSegment().toCharArray();
                for (int i=0; i<chars.length; i++)
                {
                    if (Character.isHighSurrogate(chars[i])) {
                        errs.add(new BatchException.TuvError(
                            tuv, "lb_import_tm_tuv_error_high_unicode",
                            "Contains high unicode characters"));
                        badTu = true;
                    }
                }
            }
            if (! badTu) {
                goodSegments.add(tu);
            }
        }

        TuvMappingHolder holder = null;
        
        Session session = TmUtil.getStableSession();
        try
        {
            TmPopulator tmPopulator = new TmPopulator(session);
            holder = tmPopulator.saveSegmentToSegmentTm(
                goodSegments, tm, p_mode, p_sourceTmName);
        }
        catch(LingManagerException le)
        {
            throw le;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }

        if (errs.size() > 0) {
            throw new BatchException(errs);
        }
        return holder;
    }

    /**
     * Updates existing TUVs in a Segment Tm.
     * 
     * @param p_tmId
     *            Tm id in which segments are updated
     * @param p_tuvs
     *            Collection of SegmentTmTuv objects.
     * 
     * @throws LingManagerException
     */
    @Override
    public void updateSegmentTmTuvs(Tm tm, Collection p_tuvs)
        throws LingManagerException
    {
        if (p_tuvs.size() == 0)
        {
            return;
        }

        Session session = TmUtil.getStableSession();
        Transaction tx = null;
        try
        {
            tx = session.beginTransaction();
            tm.getSegmentTmInfo().updateSegmentTmTuvs(session, tm, p_tuvs);
            tx.commit();
        }
        catch (LingManagerException le)
        {
            le.printStackTrace();
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                }
                catch (Exception e2) { /* preserve original exception */ }
            }
            throw le;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                }
                catch (Exception e2) { /* preserve original exception */ }
            }
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }

    /**
     * Updates a single existing TUV in a Segment Tm.
     * 
     * @param p_tmId
     *            Tm id in which segments are updated
     * @param p_tuv
     *            the SegmentTmTuv object to update.
     * 
     * @throws LingManagerException
     */
    @Override
    public void updateSegmentTmTuv(Tm tm, BaseTmTuv p_tuv)
        throws LingManagerException
    {
    }

    /**
     * Deletes Tuvs in the Gold Tm.
     * 
     * @param p_tuvs
     *            Tuvs (SegmentTmTuv) to be deleted.
     */
    @Override
    public void deleteSegmentTmTuvs(Tm p_tm, Collection<SegmentTmTuv> p_tuvs)
            throws LingManagerException
    {
        if (p_tuvs.size() == 0)
        {
            return;
        }

        Session session = TmUtil.getStableSession();
        Transaction tx = null;
        try
        {
            tx = session.beginTransaction();
            p_tm.getSegmentTmInfo().deleteSegmentTmTuvs(session, p_tm, p_tuvs);
            tx.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                }
                catch (Exception e2) { /* preserve original exception */ }
            }
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }

    /**
     * Deletes Tus and their Tuvs in the Gold Tm.
     * 
     * @param p_tus
     *            Tus (SegmentTmTu) to be deleted.
     */
    public void deleteSegmentTmTus(Tm p_tm, Collection<SegmentTmTu> p_tus)
            throws LingManagerException
    {
        if (p_tus.size() == 0)
        {
            return;
        }

        Session session = TmUtil.getStableSession();
        Transaction tx = null;
        try
        {
            tx = session.beginTransaction();
            p_tm.getSegmentTmInfo().deleteSegmentTmTus(session, p_tm, p_tus);
            tx.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                }
                catch (Exception e2) { /* preserve original exception */ }
            }
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }

    //
    // Stubs created during the refactoring process
    //

    /**
     * Remove TM data.  This will first perform common corpus cleaning code
     * followed by calling segment tm-specific cleaning code.
     */
    @Override
    public boolean removeTmData(Tm pTm, ProgressReporter pReporter,
            InterruptMonitor pMonitor) throws RemoteException,
            LingManagerException {
        
        pReporter.setMessageKey("lb_tm_remove_removing_corpus_tm", "Removing Corpus Tm...");
        pReporter.setPercentage(10);
        Session session = TmUtil.getStableSession();        
        Transaction tx = null;
        try {
            // WARNING: About transactions and this bit.
            // Everything here is kept in a single transaction except for this statement.
            // The reason is that removeCorpus() does two things:
            // 1) Explicit JDBC query on the passed connection to fetch some corpus ids
            // 2) Pass the corpus IDs to the CorpusManagerLocal, which deletes them by 
            //    OPENING A SEPARATE SESSION AND TRANSACTION FOR EACH. 
            // Since the Hibernate session mgmt is global, this would trample our transaction.
            // For now, we just do this stuff in its own (bad) individual transactions.  
            TmRemoveHelper.removeCorpus(session.connection(), pTm.getId(), null);
            
            tx = session.beginTransaction();
            boolean success = pTm.getSegmentTmInfo()
                .removeTmData(session, pTm, pReporter, pMonitor);
            if (success) {
                pReporter.setMessageKey("lb_tm_remove_removing_tm", 
                        "Removing Tm ...");
                pReporter.setPercentage(90);
                TmRemoveHelper.removeTm(pTm);
                pReporter.setMessageKey("lb_tm_remove_tm_success", 
                        "Tm has been successfully removed.");
                pReporter.setPercentage(100);
            }
            tx.commit();
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                }
                catch (Exception e2) { /* preserve original exception */ }
            }
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }


    /**
     * Remove TM data.  This will first perform common corpus cleaning code
     * followed by calling segment tm-specific cleaning code.
     */
    @Override
    public boolean removeTmData(Tm pTm, GlobalSightLocale pLocale,
            ProgressReporter pReporter, InterruptMonitor pMonitor)
            throws RemoteException, LingManagerException {

        pReporter.setMessageKey("lb_tm_remove_removing_corpus_tm", "Removing Corpus Tm...");
        pReporter.setPercentage(10);

        Session session = TmUtil.getStableSession();
        Transaction tx = null;
        try {
            // NOTE: this statement is outside the transaction.  See the comment above.
            TmRemoveHelper.removeCorpus(session.connection(), pTm.getId(), pLocale.getId());
            tx = session.beginTransaction();
            boolean b = pTm.getSegmentTmInfo().removeTmData(session, pTm, pLocale, pReporter, 
                    pMonitor);
            tx.commit();
            return b;
        } catch (Exception e) {
            e.printStackTrace();
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                }
                catch (Exception e2) { /* preserve original exception */ }
            }
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }


    @Override
    public StatisticsInfo getTmStatistics(Tm pTm, Locale pUiLocale, boolean p_includeProjects) {
        Session session = TmUtil.getStableSession();
        try {
            return pTm.getSegmentTmInfo()
                .getStatistics(session, pTm, pUiLocale, p_includeProjects);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }


    @Override
    public DynamicLeverageResults leverageSegment(BaseTmTuv pTuv,
            LeverageOptions pOptions) throws RemoteException,
            LingManagerException {
        SortedTms sortedTms = sortTmsByImplementation(pOptions.getLeverageTms());
        Session session = TmUtil.getStableSession();
        try
        {
            DynamicLeverageResults results = new Leverager(session)
                .leverageSegment(pTuv, pOptions, sortedTms.tm2Tms, sortedTms.tm3Tms);
            return results;
        }
        catch(Exception e)
        {
            e.printStackTrace();

            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }

    /**
     * Code refactored from BrowseCorpusMainHandler and Ambassador webservice
     * implementation, then split up to handle mixed tm2/tm3 TMs.
     */
    @Override
    public LeverageDataCenter leverageSegments(List<? extends BaseTmTuv> p_tuvs,
            GlobalSightLocale p_srcLocale, List<GlobalSightLocale> p_tgtLocales,
            LeverageOptions p_options)
        throws RemoteException, LingManagerException {
        
        LeverageDataCenter leverageDataCenter =
            new LeverageDataCenter(p_srcLocale, p_tgtLocales, p_options);
        for (BaseTmTuv tuv : p_tuvs) {
            leverageDataCenter.addOriginalSourceTuv(tuv);
        }

        // Split the tms up by implementation
        SortedTms sortedTms = sortTmsByImplementation(p_options.getLeverageTms());
        
        Session session = TmUtil.getStableSession();
        try
        {
            LeverageMatchResults levMatchResult = new LeverageMatchResults();
            if (sortedTms.tm2Tms.size() > 0) {
                levMatchResult = new Tm2SegmentTmInfo()
                        .leverage(session, sortedTms.tm2Tms, leverageDataCenter);
            }
            if (sortedTms.tm3Tms.size() > 0) {
                levMatchResult.merge(new Tm3SegmentTmInfo().leverage(session, 
                                sortedTms.tm3Tms, leverageDataCenter));
            }
            leverageDataCenter
                .addLeverageResultsOfSegmentTmMatching(levMatchResult);            
            leverageDataCenter.applySegmentTmOptions();
        }
        catch(LingManagerException le)
        {
            throw le;
        }
        catch(Exception e)
        {
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
        return leverageDataCenter;
    }

    @Override
    public List<SegmentTmTu> getSegmentsById(List<TMidTUid> tuIds)
        throws LingManagerException {
        Session session = TmUtil.getStableSession();
        try {
            Map<Long, List<Long>> tuIdsByTmId = new HashMap<Long, List<Long>>();

            for (TMidTUid id: tuIds) {
                if (! tuIdsByTmId.containsKey(id.getTmId())) {
                    tuIdsByTmId.put(id.getTmId(), new ArrayList<Long>());
                }
                tuIdsByTmId.get(id.getTmId()).add(id.getTuId());
            }

            ProjectHandler ph = ServerProxy.getProjectHandler();
            Map<TMidTUid, SegmentTmTu> result =
                new HashMap<TMidTUid, SegmentTmTu>();
            for (Map.Entry<Long, List<Long>> e: tuIdsByTmId.entrySet()) {
                Tm tm = ph.getProjectTMById(e.getKey(), false);
                List<SegmentTmTu> tus = tm.getSegmentTmInfo().getSegmentsById(
                        session, tm, e.getValue());
                for (SegmentTmTu tu: tus) {
                    result.put(new TMidTUid(tm.getId(), tu.getId()), tu);
                }
            }

            ArrayList<SegmentTmTu> orderedResult =
                new ArrayList<SegmentTmTu>(tuIds.size());
            for (TMidTUid tuId: tuIds) {
                orderedResult.add(result.get(tuId));
            }
            return orderedResult;
        }
        catch (Exception e) {
            throw new LingManagerException(e);
        }
        finally {
            TmUtil.closeStableSession(session);
        }
    }
    
    /**
     * WARNING: This routine will leak a session object to the SegmentResultSet
     * that it creates.   The caller must call SegmentResultSet.finish() in order
     * to clean it up.
     */
    @Override
    public SegmentResultSet getAllSegments(Tm tm, String createdBefore,
            String createdAfter) throws RemoteException, LingManagerException {
        Session session = TmUtil.getStableSession();
        return getInfo(tm).getAllSegments(session, 
                            tm, createdBefore, createdAfter);
    }


    /**
     * WARNING: This routine will leak a session object to the SegmentResultSet
     * that it creates.   The caller must call SegmentResultSet.finish() in order
     * to clean it up.
     */
    @Override
    public SegmentResultSet  getSegmentsByLocale(Tm tm, String locale,
            String createdBefore, String createdAfter) throws RemoteException,
            LingManagerException {
        Session session = TmUtil.getStableSession();
        return getInfo(tm).getSegmentsByLocale(session, 
                tm, locale, createdBefore, createdAfter);
    }

    /**
     * WARNING: This routine will leak a session object to the SegmentResultSet
     * that it creates.   The caller must call SegmentResultSet.finish() in order
     * to clean it up.
     */
    @Override
    public SegmentResultSet  getSegmentsByProjectName(Tm tm,
            String projectName, String createdBefore, String createdAfter)
            throws RemoteException, LingManagerException {
        Session session = TmUtil.getStableSession();
        return getInfo(tm).getSegmentsByProjectName(session, 
                tm, projectName, createdBefore, createdAfter);
    }

    @Override
    public int  getAllSegmentsCount(Tm tm, String createdBefore,
            String createdAfter) throws RemoteException, LingManagerException {
        Session session = TmUtil.getStableSession();
        try {
            return getInfo(tm).getAllSegmentsCount(session, 
                            tm, createdBefore, createdAfter);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }


    @Override
    public int  getSegmentsCountByLocale(Tm tm, String locale,
            String createdBefore, String createdAfter) throws RemoteException,
            LingManagerException {
        Session session = TmUtil.getStableSession();
        try {
            return getInfo(tm).getSegmentsCountByLocale(session, 
                    tm, locale, createdBefore, createdAfter);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }


    @Override
    public int getSegmentsCountByProjectName(Tm tm,
            String projectName, String createdBefore, String createdAfter)
            throws RemoteException, LingManagerException {
        Session session = TmUtil.getStableSession();
        try {
            return getInfo(tm).getSegmentsCountByProjectName(session, 
                    tm, projectName, createdBefore, createdAfter);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }

    
    // TODO: this needs to have session handling code added
    // tmPriority param is stupid--just pass the tms in order!
    @Override
    public List<TMidTUid> tmConcordanceQuery(List<Tm> tms, String query, 
        GlobalSightLocale sourceLocale, GlobalSightLocale targetLocale,
        final Map<Tm, Integer> tmPriority)
        throws RemoteException, LingManagerException {
        List<Tm> sortedTms = new ArrayList<Tm>(tms);
        Collections.sort(sortedTms, new Comparator<Tm>() {
            private int getPriority(Tm tm) {
                if (tmPriority == null) {
                    return 0;
                }
                Integer priority = tmPriority.get(tm);
                return priority == null ? 0 : priority;
            }
            public int compare(Tm tm1, Tm tm2) {
                return getPriority(tm1) - getPriority(tm2);
            }

        });

        // Could batch up by TM implementation, but it's more complex and
        // probably no faster currently
        List<TMidTUid> result = new ArrayList<TMidTUid>();
        for (Tm tm: sortedTms) {
            result.addAll(tm.getSegmentTmInfo().tmConcordanceQuery(
                Collections.singletonList(tm),
                query, sourceLocale, targetLocale));
        }

        return result;
    }

    @Override
    public Set<GlobalSightLocale> getTmLocales(Tm tm)
            throws LingManagerException {
        Session session = TmUtil.getStableSession();
        try {
            return getInfo(tm).getLocalesForTm(session, tm);
        } catch (Exception e) {
            e.printStackTrace();
            throw new LingManagerException(e);
        }
        finally {
            if (session != null) {
                TmUtil.closeStableSession(session);
            }
        }
    }
    
    private SegmentTmInfo getInfo(Tm tm) {
        if (tm == null) {
            // DEBUG
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            // Walk upwards to find the first non-SQLUtil caller (skip 0, which is Thread)
            for (int i = 1; i < stack.length; i++) { 
                System.out.println(stack[i].toString());
            }
            // !DEBUG
            throw new IllegalArgumentException("Null tm");
        }
        return tm.getSegmentTmInfo();
    }
    
    /**
     * WARNING: This routine will leak a session object to the Reindexer
     * that it creates.   The caller must clean it up by calling 
     * TmUtil.closeStableSession().
     */
    @Override
    public Reindexer getReindexer(Collection<ProjectTM> tms) throws LingManagerException {
        if (tms.size() == 0) {
            throw new IllegalArgumentException();
        }
        
        // Leak this session to Reindexer, which runs in its own thread
        Session session = TmUtil.getStableSession();
        try {
            // Weed out any remote tms, since they don't get reindexed
            SortedTms sorted = sortTmsByImplementation(tms);
            sorted.tm2Tms.addAll(sorted.tm3Tms);
            return new Reindexer(session, sorted.tm2Tms);
        } catch (Exception e) {
            throw new LingManagerException(e);
        }
    }

    @Override
    public String getCreatingUserByTuvId(long tmId, long tuvId)
            throws RemoteException, LingManagerException {
        try {
            Tm tm = ServerProxy.getProjectHandler().getProjectTMById(tmId, false);

            Session session = TmUtil.getStableSession();
            try {
                return getInfo(tm).getCreatingUserByTuvId(session, tm, tuvId);
            } catch (Exception e) {
                throw new LingManagerException(e);
            }
            finally {
                if (session != null) {
                    TmUtil.closeStableSession(session);
                }
            }
        }
        catch (NamingException e) {
            throw new LingManagerException(e);
        }
    }


    @Override
    public Date getModifyDateByTuvId(long tmId, long tuvId)
            throws RemoteException, LingManagerException {
        try {
            Tm tm = ServerProxy.getProjectHandler().getProjectTMById(tmId, false);
            
            Session session = TmUtil.getStableSession();
            try {
                return getInfo(tm).getModifyDateByTuvId(session, tm, tuvId);
            } catch (Exception e) {
                throw new LingManagerException(e);
            }
            finally {
                if (session != null) {
                    TmUtil.closeStableSession(session);
                }
            }
        }
        catch (NamingException e) {
            throw new LingManagerException(e);
        }

    }


    @Override
    public String getSidByTuvId(long tmId, long tuvId) throws RemoteException,
            LingManagerException {
        try {
            Tm tm = ServerProxy.getProjectHandler().getProjectTMById(tmId, false);
            if (tm == null) {
                throw new IllegalArgumentException("No such tmId " + tmId);
            }
            Session session = TmUtil.getStableSession();
            try {
                return getInfo(tm).getSidByTuvId(session, tm, tuvId);
            } catch (Exception e) {
                throw new LingManagerException(e);
            }
            finally {
                if (session != null) {
                    TmUtil.closeStableSession(session);
                }
            }
        }
        catch (NamingException e) {
            throw new LingManagerException(e);
        }

    }


    @Override
    public String getSourceTextByTuvId(long tmId, long tuvId, long srcLocaleId)
            throws RemoteException, LingManagerException {
        try {
            Tm tm = ServerProxy.getProjectHandler().getProjectTMById(tmId, false);
            
            Session session = TmUtil.getStableSession();
            try {
                return getInfo(tm).getSourceTextByTuvId(session, tm, tuvId, srcLocaleId);
            } catch (Exception e) {
                throw new LingManagerException(e);
            }
            finally {
                if (session != null) {
                    TmUtil.closeStableSession(session);
                }
            }
        }
        catch (NamingException e) {
            throw new LingManagerException(e);
        }
    }

    private static class SortedTms {
        List<Tm> tm2Tms = new ArrayList<Tm>();
        List<Tm> tm3Tms = new ArrayList<Tm>();
        List<Tm> remoteTms = new ArrayList<Tm>();
    }
    
    private SortedTms sortTmsByImplementation(Collection<? extends Tm> tms) {
        SortedTms sorted = new SortedTms();
        for (Tm tm : tms) {
            if (tm.getIsRemoteTm()) {
                sorted.remoteTms.add(tm);
            }
            else if (tm.getTm3Id() != null) {
                sorted.tm3Tms.add(tm);
            }
            else {
                sorted.tm2Tms.add(tm);
            }
        }
        return sorted;
    }
}
