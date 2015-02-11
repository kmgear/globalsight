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
package com.globalsight.everest.webapp.pagehandler.administration.reports.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.globalsight.everest.comment.CommentManager;
import com.globalsight.everest.comment.Issue;
import com.globalsight.everest.comment.IssueHistory;
import com.globalsight.everest.comment.IssueImpl;
import com.globalsight.everest.comment.IssueOptions;
import com.globalsight.everest.company.CompanyThreadLocal;
import com.globalsight.everest.company.CompanyWrapper;
import com.globalsight.everest.edit.CommentHelper;
import com.globalsight.everest.integration.ling.LingServerProxy;
import com.globalsight.everest.integration.ling.tm2.LeverageMatch;
import com.globalsight.everest.jobhandler.Job;
import com.globalsight.everest.page.SourcePage;
import com.globalsight.everest.page.TargetPage;
import com.globalsight.everest.persistence.tuv.SegmentTuUtil;
import com.globalsight.everest.projecthandler.TranslationMemoryProfile;
import com.globalsight.everest.servlet.util.ServerProxy;
import com.globalsight.everest.tuv.Tuv;
import com.globalsight.everest.util.comparator.GlobalSightLocaleComparator;
import com.globalsight.everest.webapp.WebAppConstants;
import com.globalsight.everest.webapp.pagehandler.administration.company.Select;
import com.globalsight.everest.webapp.pagehandler.administration.reports.ReportConstants;
import com.globalsight.everest.webapp.pagehandler.administration.reports.ReportHelper;
import com.globalsight.everest.webapp.pagehandler.administration.users.UserUtil;
import com.globalsight.everest.workflowmanager.Workflow;
import com.globalsight.ling.tm.LeverageMatchLingManager;
import com.globalsight.ling.tw.PseudoConstants;
import com.globalsight.ling.tw.PseudoData;
import com.globalsight.ling.tw.TmxPseudo;
import com.globalsight.terminology.ITermbase;
import com.globalsight.terminology.ITermbaseManager;
import com.globalsight.terminology.termleverager.TermLeverageManager;
import com.globalsight.terminology.termleverager.TermLeverageMatch;
import com.globalsight.terminology.termleverager.TermLeverageOptions;
import com.globalsight.util.GeneralException;
import com.globalsight.util.GlobalSightLocale;
import com.globalsight.util.StringUtil;
import com.globalsight.util.edit.EditUtil;
import com.globalsight.util.resourcebundle.ResourceBundleConstants;
import com.globalsight.util.resourcebundle.SystemResourceBundle;

/**
 * This Generator is used for creating Translations Edit Report (Include
 * Translations Edit Report in offline download report page)
 */
public class TranslationsEditReportGenerator implements ReportGenerator,
        Cancelable
{
    private static final Logger logger = 
            Logger.getLogger(TranslationsEditReportGenerator.class);

    private static final String CATEGORY_FAILURE_DROP_DOWN_LIST = "categoryFailureDropDownList";

    private CellStyle headerStyle = null;
    private CellStyle contentStyle = null;
    private CellStyle rtlContentStyle = null;
    private CellStyle unlockedStyle = null;
    private CellStyle unlockedRightStyle = null;

    public static final int LANGUAGE_HEADER_ROW = 3;
    public static final int LANGUAGE_INFO_ROW = 4;
    public static final int SEGMENT_HEADER_ROW = 6;
    public static final int SEGMENT_START_ROW = 7;
    // "L" column, index 11
    public static final int CATEGORY_FAILURE_COLUMN = 11;

    private Locale m_uiLocale;
    private String m_companyName = "";
    protected List<Long> m_jobIDS = new ArrayList<Long>();
    protected List<GlobalSightLocale> m_targetLocales = new ArrayList<GlobalSightLocale>();
    private String m_dateFormat;
    private ResourceBundle m_bundle;

    private boolean cancel = false;

    public TranslationsEditReportGenerator(String p_currentCompanyName)
    {
        m_companyName = p_currentCompanyName;
        CompanyThreadLocal.getInstance().setValue(m_companyName);
        m_uiLocale = Locale.US;
        m_bundle = SystemResourceBundle.getInstance().getResourceBundle(
                ResourceBundleConstants.LOCALE_RESOURCE_NAME, m_uiLocale);
    }

    /**
     * Constructor.
     * 
     * @param p_request
     *            the request
     * @param p_response
     *            the response
     * @throws Exception
     */
    public TranslationsEditReportGenerator(HttpServletRequest p_request,
            HttpServletResponse p_response) throws Exception
    {
        m_uiLocale = (Locale) p_request.getSession().getAttribute(
                WebAppConstants.UILOCALE);
        if (m_uiLocale == null)
        {
            m_uiLocale = Locale.US;
        }
        m_bundle = SystemResourceBundle.getInstance().getResourceBundle(
                ResourceBundleConstants.LOCALE_RESOURCE_NAME, m_uiLocale);

        m_dateFormat = p_request.getParameter(WebAppConstants.DATE_FORMAT);
        if (m_dateFormat == null)
        {
            m_dateFormat = DEFAULT_DATE_FORMAT;
        }

        m_jobIDS = ReportHelper.getListOfLong(p_request
                .getParameter(ReportConstants.JOB_IDS));
        GlobalSightLocaleComparator comparator = new GlobalSightLocaleComparator(
                GlobalSightLocaleComparator.ISO_CODE, m_uiLocale);
        m_targetLocales = ReportHelper
                .getTargetLocaleList(p_request
                        .getParameterValues(ReportConstants.TARGETLOCALE_LIST),
                        comparator);
        
        m_companyName = UserUtil.getCurrentCompanyName(p_request);
        if (CompanyWrapper.isSuperCompanyName(m_companyName)
                && m_jobIDS != null && m_jobIDS.size() > 0)
        {
            Job job = ServerProxy.getJobHandler().getJobById(m_jobIDS.get(0));
            m_companyName = CompanyWrapper.getCompanyNameById(job.getCompanyId());
        }
        CompanyThreadLocal.getInstance().setValue(m_companyName);
    }

    @Override
    public File[] generateReports(List<Long> p_jobIDS,
            List<GlobalSightLocale> p_targetLocales) throws Exception
    {
        List<File> workBooks = new ArrayList<File>();
        for (long jobID : p_jobIDS)
        {
            if (cancel)
                return new File[0];

            Job job = ServerProxy.getJobHandler().getJobById(jobID);
            if (job == null)
                continue;

            File file = ReportHelper.getXLSReportFile(getReportType(), job);

            Workbook workBook = new XSSFWorkbook();
            createReport(workBook, job, p_targetLocales, m_dateFormat);

            FileOutputStream out = new FileOutputStream(file);
            workBook.write(out);
            out.close();

            workBooks.add(file);
        }

        return workBooks.toArray(new File[workBooks.size()]);
    }

    /**
     * Create the report
     * 
     * @throws Exception
     */
    private void createReport(Workbook p_workbook, Job p_job,
            List<GlobalSightLocale> p_targetLocales, String p_dateFormat)
            throws Exception
    {
        // Till now, only support one target locale.
        GlobalSightLocale trgLocale = p_targetLocales.get(0);

        // Create Sheet
        Sheet sheet = p_workbook.createSheet(m_bundle.getString("lb_ter"));
        sheet.protectSheet("");

        // Add Title
        addTitle(p_workbook, sheet);

        // Add Locale Pair Header
        addLanguageHeader(p_workbook, sheet);

        // Add Segment Header
        addSegmentHeader(p_workbook, sheet);

        // Create Name Areas for drop down list.
        createCategoryFailureNameArea(p_workbook);

        // Insert Data into Report
        String srcLang = p_job.getSourceLocale().getDisplayName(m_uiLocale);
        String trgLang = trgLocale.getDisplayName(m_uiLocale);
        writeLanguageInfo(p_workbook, sheet, srcLang, trgLang);

        writeSegmentInfo(p_workbook, sheet, p_job, trgLocale, "", p_dateFormat,
                SEGMENT_START_ROW);
    }

    /**
     * Add title to the sheet
     * 
     * @param p_workBook
     * @param p_sheet
     *            the sheet
     * @throws Exception
     */
    private void addTitle(Workbook p_workBook, Sheet p_sheet) throws Exception
    {
        Font titleFont = p_workBook.createFont();
        titleFont.setUnderline(Font.U_NONE);
        titleFont.setFontName("Times");
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        CellStyle cs = p_workBook.createCellStyle();
        cs.setFont(titleFont);

        Row titleRow = getRow(p_sheet, 0);
        Cell titleCell = getCell(titleRow, 0);
        titleCell.setCellValue(m_bundle.getString("lb_translation_edit_report"));
        titleCell.setCellStyle(cs);
    }

    /**
     * Add job header to the sheet
     * 
     * @param p_workBook
     * @param p_sheet
     *            the sheet
     * @throws Exception
     */
    private void addLanguageHeader(Workbook p_workBook, Sheet p_sheet)
            throws Exception
    {
        int col = 0;
        int row = LANGUAGE_HEADER_ROW;

        Row langRow = getRow(p_sheet, row);
        Cell srcLangCell = getCell(langRow, col);
        srcLangCell.setCellValue(m_bundle.getString("lb_source_language"));
        srcLangCell.setCellStyle(getHeaderStyle(p_workBook));
        col++;

        Cell trgLangCell = getCell(langRow, col);
        trgLangCell.setCellValue(m_bundle.getString("lb_target_language"));
        trgLangCell.setCellStyle(getHeaderStyle(p_workBook));
    }

    /**
     * Add segment header to the sheet
     * 
     * @param p_workBook
     * @param p_sheet
     *            the sheet
     * @throws Exception
     */
    private void addSegmentHeader(Workbook p_workBook, Sheet p_sheet)
            throws Exception
    {
        int col = 0;
        int row = SEGMENT_HEADER_ROW;
        Row segHeaderRow = getRow(p_sheet, row);
        CellStyle headerStyle = getHeaderStyle(p_workBook);

        Cell cell_A = getCell(segHeaderRow, col);
        cell_A.setCellValue(m_bundle.getString("lb_job_id_report"));
        cell_A.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 20 * 256);
        col++;

        Cell cell_B = getCell(segHeaderRow, col);
        cell_B.setCellValue(m_bundle.getString("lb_segment_id"));
        cell_B.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 20 * 256);
        col++;

        Cell cell_C = getCell(segHeaderRow, col);
        cell_C.setCellValue(m_bundle.getString("lb_targetpage_id"));
        cell_C.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 20 * 256);
        col++;

        Cell cell_D = getCell(segHeaderRow, col);
        cell_D.setCellValue(m_bundle.getString("lb_source_segment"));
        cell_D.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_E = getCell(segHeaderRow, col);
        cell_E.setCellValue(m_bundle.getString("lb_target_segment"));
        cell_E.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_F = getCell(segHeaderRow, col);
        cell_F.setCellValue(m_bundle.getString("lb_sid"));
        cell_F.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_G = getCell(segHeaderRow, col);
        cell_G.setCellValue(m_bundle.getString("lb_source_segment_with_compact_tags"));
        cell_G.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_H = getCell(segHeaderRow, col);
        cell_H.setCellValue(m_bundle.getString("lb_target_segment_with_compact_tags"));
        cell_H.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_I = getCell(segHeaderRow, col);
        cell_I.setCellValue(m_bundle.getString("lb_required_translation"));
        cell_I.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_J = getCell(segHeaderRow, col);
        cell_J.setCellValue(m_bundle.getString("lb_comments"));
        cell_J.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_K = getCell(segHeaderRow, col);
        cell_K.setCellValue(m_bundle.getString("lb_required_comment"));
        cell_K.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_L = getCell(segHeaderRow, col);
        cell_L.setCellValue(m_bundle.getString("lb_category_failure"));
        cell_L.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 40 * 256);
        col++;

        Cell cell_M = getCell(segHeaderRow, col);
        cell_M.setCellValue(m_bundle.getString("lb_comment_status"));
        cell_M.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 30 * 256);
        col++;

        Cell cell_N = getCell(segHeaderRow, col);
        cell_N.setCellValue(m_bundle.getString("lb_tm_match_original"));
        cell_N.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 30 * 256);
        col++;

        Cell cell_O = getCell(segHeaderRow, col);
        cell_O.setCellValue(m_bundle.getString("lb_glossary_source"));
        cell_O.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 30 * 256);
        col++;

        Cell cell_P = getCell(segHeaderRow, col);
        cell_P.setCellValue(m_bundle.getString("lb_glossary_target"));
        cell_P.setCellStyle(headerStyle);
        p_sheet.setColumnWidth(col, 30 * 256);
    }

    private void writeLanguageInfo(Workbook p_workbook, Sheet p_sheet,
            String p_sourceLang, String p_targetLang) throws Exception
    {
        int col = 0;
        int row = LANGUAGE_INFO_ROW;
        CellStyle contentStyle = getContentStyle(p_workbook);
        Row langInfoRow = getRow(p_sheet, row);

        // Source Language
        Cell srcLangCell = getCell(langInfoRow, col++);
        srcLangCell.setCellValue(p_sourceLang);
        srcLangCell.setCellStyle(contentStyle);

        // Target Language
        Cell trgLangCell = getCell(langInfoRow, col++);
        trgLangCell.setCellValue(p_targetLang);
        trgLangCell.setCellStyle(contentStyle);
    }

    /**
     * For Translations Edit Report, Write segment information into each row of
     * the sheet.
     * 
     * @param p_sheet
     *            the sheet
     * @param p_jobId
     *            the job id
     * @param p_targetLang
     *            the target locale String
     * @param p_srcPageId
     *            the source page id
     * @param p_dateFormat
     *            the data format
     * @param p_row
     *            the segment row in sheet
     */
    private int writeSegmentInfo(Workbook p_workBook, Sheet p_sheet, Job p_job,
            GlobalSightLocale p_targetLocale, String p_srcPageId, String p_dateFormat,
            int p_row) throws Exception
    {
        long companyId = p_job.getCompanyId();
        Vector<TargetPage> targetPages = new Vector<TargetPage>();

        TranslationMemoryProfile tmp = null;
        List<String> excludItems = null;

        for (Workflow workflow : p_job.getWorkflows())
        {
            if (cancel)
                return 0;

            if (Workflow.PENDING.equals(workflow.getState())
                    || Workflow.CANCELLED.equals(workflow.getState())
                    // || Workflow.EXPORT_FAILED.equals(workflow.getState())
                    || Workflow.IMPORT_FAILED.equals(workflow.getState()))
            {
                continue;
            }
            if (p_targetLocale.getId() == workflow.getTargetLocale().getId())
            {
                targetPages = workflow.getTargetPages();
                tmp = workflow.getJob().getL10nProfile()
                        .getTranslationMemoryProfile();
                if (tmp != null)
                {
                    excludItems = new ArrayList<String>(
                            tmp.getJobExcludeTuTypes());
                }
            }
        }

        if (targetPages.isEmpty())
        {
            // If no corresponding target page exists, set the cell blank
            writeBlank(p_sheet, p_row, 11);
        }
        else
        {
            LeverageMatchLingManager leverageMatchLingManager = LingServerProxy
                    .getLeverageMatchLingManager();
            TermLeverageManager termLeverageManager = ServerProxy
                    .getTermLeverageManager();
            CommentManager commentManager = ServerProxy.getCommentManager();

            Locale sourcePageLocale = p_job.getSourceLocale().getLocale();
            Locale targetPageLocale = p_targetLocale.getLocale();
            TermLeverageOptions termLeverageOptions = getTermLeverageOptions(
                    sourcePageLocale, targetPageLocale, p_job
                            .getL10nProfile().getProject()
                            .getTermbaseName(),
                    String.valueOf(p_job.getCompanyId()));
            Map<Long, Set<TermLeverageMatch>> termLeverageMatchResultMap = null;
            if (termLeverageOptions != null)
            {
                termLeverageMatchResultMap = termLeverageManager
                        .getTermMatchesForPages(
                                new HashSet<SourcePage>(p_job.getSourcePages()),
                                p_targetLocale);
            }

            String category = null;
            PseudoData pData = new PseudoData();
            pData.setMode(PseudoConstants.PSEUDO_COMPACT);
            String sourceSegmentString = null;
            String targetSegmentString = null;
            String sid = null;
            for (int i = 0; i < targetPages.size(); i++)
            {
                if (cancel)
                    return 0;

                TargetPage targetPage = (TargetPage) targetPages.get(i);
                SourcePage sourcePage = targetPage.getSourcePage();

                if (!"".equals(p_srcPageId)
                        && !p_srcPageId.equals(String.valueOf(sourcePage
                                .getId())))
                {
                    // ignore the source pages not equal to the one
                    // if the request comes from pop up editor
                    continue;
                }

                SegmentTuUtil.getTusBySourcePageId(sourcePage.getId());
                List sourceTuvs = getPageTuvs(sourcePage);
                List targetTuvs = getPageTuvs(targetPage);
                Map exactMatches = leverageMatchLingManager.getExactMatches(
                        sourcePage.getIdAsLong(),
                        new Long(targetPage.getLocaleId()));
                Map<Long, Set<LeverageMatch>> leverageMatcheMap = leverageMatchLingManager
                        .getFuzzyMatches(sourcePage.getIdAsLong(), new Long(
                                targetPage.getLocaleId()));

                sourcePageLocale = sourcePage.getGlobalSightLocale()
                        .getLocale();
                targetPageLocale = targetPage.getGlobalSightLocale()
                        .getLocale();

                boolean m_rtlSourceLocale = EditUtil
                        .isRTLLocale(sourcePageLocale.toString());
                boolean m_rtlTargetLocale = EditUtil
                        .isRTLLocale(targetPageLocale.toString());

                // Find segment all comments belong to this target page
                List<IssueImpl> issues = commentManager.getIssues(
                        Issue.TYPE_SEGMENT, targetPage.getId());

                for (int j = 0; j < targetTuvs.size(); j++)
                {
                    if (cancel)
                        return 0;

                    int col = 0;
                    Tuv targetTuv = (Tuv) targetTuvs.get(j);
                    Tuv sourceTuv = (Tuv) sourceTuvs.get(j);
                    String targetLanguage = targetTuv.getGlobalSightLocale()
                            .getLanguage();

                    category = sourceTuv.getTu(companyId).getTuType();
                    if (excludItems != null && excludItems.contains(category))
                    {
                        continue;
                    }

                    // Comment
                    List issueHistories = null;
                    String lastComment = "";
                    String failure = "";
                    String commentStatus = "";
                    if (issues != null)
                    {
                        for (int m = 0; m < issues.size(); m++)
                        {
                            if (cancel)
                                return 0;

                            Issue issue = (Issue) issues.get(m);
                            String logicKey = CommentHelper.makeLogicalKey(
                                    targetPage.getId(),
                                    targetTuv.getTu(companyId).getId(),
                                    targetTuv.getId(), 0);

                            if (logicKey.equals(issue.getLogicalKey()))
                            {
                                issueHistories = issue.getHistory();
                                failure = issue.getCategory();
                                commentStatus = issue.getStatus();
                                break;
                            }
                        }
                    }
                    if (issueHistories != null && issueHistories.size() > 0)
                    {
                        IssueHistory issueHistory = (IssueHistory) issueHistories
                                .get(0);
                        lastComment = issueHistory.getComment();
                    }

                    sourceSegmentString = sourceTuv.getGxmlElement()
                            .getTextValue();
                    sid = sourceTuv.getSid();
                    targetSegmentString = targetTuv.getGxmlElement()
                            .getTextValue();

                    String matches = "";
                    Set<LeverageMatch> leverageMatches = (Set<LeverageMatch>) leverageMatcheMap
                            .get(sourceTuv.getIdAsLong());
                    if (exactMatches.get(sourceTuv.getIdAsLong()) != null)
                    {
                        matches = StringUtil.formatPCT(100);
                    }
                    else if (leverageMatches != null)
                    {
                        int count = 0;
                        for (LeverageMatch leverageMatch : leverageMatches)
                        {
                            if (cancel)
                                return 0;

                            if ((leverageMatches.size() > 1))
                            {
                                matches = matches
                                        + (++count)
                                        + ", "
                                        + StringUtil.formatPCT(leverageMatch
                                                .getScoreNum()) + "\r\n";
                            }
                            else
                            {
                                matches = matches
                                        + StringUtil.formatPCT(leverageMatch
                                                .getScoreNum());
                                break;
                            }

                        }
                    }
                    else
                    {
                        matches = m_bundle.getString("lb_no_match_report");
                    }
                    if (targetTuv.isRepeated())
                    {
                        matches += "\r\n"
                                + m_bundle
                                        .getString("jobinfo.tradosmatches.invoice.repeated");
                    }
                    else if (targetTuv.getRepetitionOfId() > 0)
                    {
                        matches += "\r\n"
                                + m_bundle
                                        .getString("jobinfo.tradosmatches.invoice.repetition");
                    }

                    // Get Terminology/Glossary Source and Target.
                    String sourceTerms = "";
                    String targetTerms = "";
                    if (termLeverageMatchResultMap != null
                            && termLeverageMatchResultMap.size() > 0)
                    {
                        Set<TermLeverageMatch> termLeverageMatchSet = termLeverageMatchResultMap
                                .get(sourceTuv.getId());
                        if (termLeverageMatchSet != null
                                && termLeverageMatchSet.size() > 0)
                        {
                            TermLeverageMatch tlm = termLeverageMatchSet
                                    .iterator().next();
                            sourceTerms = tlm.getMatchedSourceTerm();
                            targetTerms = tlm.getMatchedTargetTerm();
                        }
                    }

                    CellStyle contentStyle = getContentStyle(p_workBook);
                    Row currentRow = getRow(p_sheet, p_row);
                    // Job id
                    Cell cell_A = getCell(currentRow, col);
                    cell_A.setCellValue(p_job.getId());
                    cell_A.setCellStyle(contentStyle);
                    col++;

                    // Segment id
                    Cell cell_B = getCell(currentRow, col);
                    cell_B.setCellValue(sourceTuv.getTu(companyId).getId());
                    cell_B.setCellStyle(contentStyle);
                    col++;

                    // TargetPage id
                    Cell cell_C = getCell(currentRow, col);
                    cell_C.setCellValue(targetPage.getId());
                    cell_C.setCellStyle(contentStyle);
                    col++;

                    // Source segment
                    CellStyle srcStyle = m_rtlSourceLocale ? getRtlContentStyle(p_workBook)
                            : contentStyle;
                    String srcContent = m_rtlSourceLocale ? EditUtil
                            .toRtlString(sourceSegmentString)
                            : sourceSegmentString;
                    Cell cell_D = getCell(currentRow, col);
                    cell_D.setCellValue(srcContent);
                    cell_D.setCellStyle(srcStyle);
                    col++;

                    // Target segment
                    CellStyle trgStyle = m_rtlTargetLocale ? getRtlContentStyle(p_workBook)
                            : contentStyle;
                    String content = m_rtlTargetLocale ? EditUtil
                            .toRtlString(targetSegmentString)
                            : targetSegmentString;
                    Cell cell_E = getCell(currentRow, col);
                    cell_E.setCellValue(content);
                    cell_E.setCellStyle(trgStyle);
                    col++;

                    // SID
                    Cell cell_F = getCell(currentRow, col);
                    cell_F.setCellValue(sid);
                    cell_F.setCellStyle(contentStyle);
                    col++;

                    // Source segment with compact tags
                    pData.setAddables(sourceTuv.getDataType(companyId));
                    TmxPseudo.tmx2Pseudo(sourceTuv.getGxmlExcludeTopTags(),
                            pData);
                    String sContent = pData.getPTagSourceString();
                    if (sContent.equalsIgnoreCase(sourceSegmentString))
                    {
                        sContent = "";
                    }
                    else if (m_rtlTargetLocale)
                    {
                        sContent = EditUtil.toRtlString(sContent);
                    }
                    Cell cell_G = getCell(currentRow, col);
                    cell_G.setCellValue(sContent);
                    cell_G.setCellStyle(srcStyle);
                    col++;

                    // Target segment with compact tags
                    pData.setAddables(targetTuv.getDataType(companyId));
                    TmxPseudo.tmx2Pseudo(targetTuv.getGxmlExcludeTopTags(),
                            pData);
                    sContent = pData.getPTagSourceString();
                    if (sContent.equalsIgnoreCase(targetSegmentString))
                    {
                        sContent = "";
                    }
                    else if (m_rtlTargetLocale)
                    {
                        sContent = EditUtil.toRtlString(sContent);
                    }
                    Cell cell_H = getCell(currentRow, col);
                    cell_H.setCellValue(sContent);
                    cell_H.setCellStyle(trgStyle);
                    col++;

                    // Required translation
                    CellStyle requiredTranslationStyle = m_rtlTargetLocale ? getUnlockedRightStyle(p_workBook)
                            : getUnlockedStyle(p_workBook);
                    Cell cell_I = getCell(currentRow, col);
                    cell_I.setCellValue("");
                    cell_I.setCellStyle(requiredTranslationStyle);
                    col++;

                    // Comments
                    Cell cell_J = getCell(currentRow, col);
                    cell_J.setCellValue(lastComment);
                    cell_J.setCellStyle(contentStyle);
                    col++;

                    // Required comment
                    Cell cell_K = getCell(currentRow, col);
                    cell_K.setCellValue("");
                    cell_K.setCellStyle(getUnlockedStyle(p_workBook));
                    col++;

                    // Category failure
                    Cell cell_L = getCell(currentRow, col);
                    cell_L.setCellValue(failure);
                    cell_L.setCellStyle(contentStyle);
                    col++;

                    // Comment Status
                    Cell cell_M = getCell(currentRow, col);
                    cell_M.setCellValue(commentStatus);
                    CellStyle commentCS = p_workBook.createCellStyle();
                    commentCS.cloneStyleFrom(contentStyle);
                    commentCS.setLocked(false);
                    cell_M.setCellStyle(commentCS);

                    // add comment status drop down list for current row.
                    String[] statusArray = getCommentStatusList(lastComment);
                    addCommentStatusValidation(p_sheet, statusArray, p_row,
                            p_row, col, col);
                    col++;

                    // TM match
                    Cell cell_N = getCell(currentRow, col);
                    cell_N.setCellValue(matches);
                    cell_N.setCellStyle(contentStyle);
                    col++;

                    // Glossary source
                    Cell cell_O = getCell(currentRow, col);
                    cell_O.setCellValue(sourceTerms);
                    cell_O.setCellStyle(contentStyle);
                    col++;

                    // Glossary target
                    Cell cell_P = getCell(currentRow, col);
                    cell_P.setCellValue(targetTerms);
                    cell_P.setCellStyle(contentStyle);

                    p_row++;
                }
            }

            // Add category failure drop down list here.
            addCategoryFailureValidation(p_sheet, SEGMENT_START_ROW, p_row,
                    CATEGORY_FAILURE_COLUMN, CATEGORY_FAILURE_COLUMN);
        }

        return p_row;
    }

    /**
     * Populates a term leverage options object.
     */
    private TermLeverageOptions getTermLeverageOptions(Locale p_sourceLocale,
            Locale p_targetLocale, String p_termbaseName, String p_companyId)
            throws Exception
    {
        TermLeverageOptions options = null;

        Locale sourceLocale = p_sourceLocale;
        Locale targetLocale = p_targetLocale;

        try
        {
            ITermbaseManager manager = ServerProxy.getTermbaseManager();
            long termbaseId;
            if (p_companyId != null)
            {
                termbaseId = manager.getTermbaseId(p_termbaseName, p_companyId);
            }
            else
            {
                termbaseId = manager.getTermbaseId(p_termbaseName);
            }

            // If termbase does not exist, return null options.
            if (termbaseId == -1)
            {
                return null;
            }

            options = new TermLeverageOptions();
            options.addTermBase(p_termbaseName);
            options.setLoadTargetTerms(true);
            options.setSaveToDatabase(false);

            // fuzzy threshold set by object constructor - use defaults.
            options.setFuzzyThreshold(0);

            ITermbase termbase = null;
            if (p_companyId != null)
            {
                termbase = manager.connect(p_termbaseName,
                        ITermbase.SYSTEM_USER, "", p_companyId);
            }
            else
            {
                termbase = manager.connect(p_termbaseName,
                        ITermbase.SYSTEM_USER, "");
            }

            // add source locale and lang names
            options.setSourcePageLocale(sourceLocale);
            ArrayList sourceLangNames = termbase
                    .getLanguagesByLocale(sourceLocale.toString());

            for (int i = 0, max = sourceLangNames.size(); i < max; i++)
            {
                String langName = (String) sourceLangNames.get(i);

                options.addSourcePageLocale2LangName(langName);
            }

            // add target locales and lang names
            ArrayList targetLangNames = termbase
                    .getLanguagesByLocale(targetLocale.toString());
            for (int i = 0, max = targetLangNames.size(); i < max; i++)
            {
                String langName = (String) targetLangNames.get(i);
                options.addTargetPageLocale2LangName(targetLocale, langName);
                options.addLangName2Locale(langName, targetLocale);
            }
        }
        catch (Exception ex)
        {
            throw new GeneralException(ex);
        }

        return options;
    }

    private CellStyle getHeaderStyle(Workbook p_workbook) throws Exception
    {
        if (headerStyle == null)
        {
            Font font = p_workbook.createFont();
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
            font.setColor(IndexedColors.BLACK.getIndex());
            font.setUnderline(Font.U_NONE);
            font.setFontName("Times");
            font.setFontHeightInPoints((short) 11);

            CellStyle cs = p_workbook.createCellStyle();
            cs.setFont(font);
            cs.setWrapText(true);
            cs.setFillPattern(CellStyle.SOLID_FOREGROUND );
            cs.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cs.setBorderTop(CellStyle.BORDER_THIN);
            cs.setBorderRight(CellStyle.BORDER_THIN);
            cs.setBorderBottom(CellStyle.BORDER_THIN);
            cs.setBorderLeft(CellStyle.BORDER_THIN);

            headerStyle = cs;
        }

        return headerStyle;
    }

    private CellStyle getContentStyle(Workbook p_workbook) throws Exception
    {
        if (contentStyle == null)
        {
            CellStyle style = p_workbook.createCellStyle();
            style.setWrapText(true);
            style.setAlignment(CellStyle.ALIGN_LEFT);
            style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
            Font font = p_workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 10);
            style.setFont(font);

            contentStyle = style;
        }

        return contentStyle;
    }

    private CellStyle getRtlContentStyle(Workbook p_workbook) throws Exception
    {
        if (rtlContentStyle == null)
        {
            Font font = p_workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 10);

            CellStyle style = p_workbook.createCellStyle();
            style.setFont(font);
            style.setWrapText(true);
            style.setAlignment(CellStyle.ALIGN_RIGHT);
            style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            rtlContentStyle = style;
        }

        return rtlContentStyle;
    }

    private CellStyle getUnlockedStyle(Workbook p_workbook) throws Exception
    {
        if (unlockedStyle == null)
        {
            Font font = p_workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 10);

            CellStyle style = p_workbook.createCellStyle();
            style.setFont(font);
            style.setLocked(false);
            style.setWrapText(true);
            style.setAlignment(CellStyle.ALIGN_LEFT);
            style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            unlockedStyle = style;
        }

        return unlockedStyle;
    }

    private CellStyle getUnlockedRightStyle(Workbook p_workbook) throws Exception
    {
        if (unlockedRightStyle == null)
        {
            Font font = p_workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 10);

            CellStyle style = p_workbook.createCellStyle();
            style.setFont(font);
            style.setLocked(false);
            style.setWrapText(true);
            style.setAlignment(CellStyle.ALIGN_RIGHT);
            style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            unlockedRightStyle = style;
        }

        return unlockedRightStyle;
    }

    private List<String> getFailureCategoriesList()
    {
        List<String> result = new ArrayList<String>();

        String currentCompanyId = CompanyThreadLocal.getInstance().getValue();
        List failureCategories = IssueOptions.getAllCategories(m_bundle, currentCompanyId);
        for (int i = 0; i < failureCategories.size(); i++)
        {
            Select aCategory = (Select) failureCategories.get(i);
            result.add(aCategory.getValue());
        }

        return result;
    }

    /**
     * Set the cell blank
     * 
     * @param p_sheet
     *            the sheet
     * @param p_row
     *            the row position
     * @param p_colLen
     *            the blank column length
     * @throws Exception
     */
    private void writeBlank(Sheet p_sheet, int p_row, int p_colLen)
            throws Exception
    {
        for (int col = 0; col < p_colLen; col++)
        {
            Row row = p_sheet.getRow(p_row);
            Cell cell = getCell(row, col);
            cell.setCellValue("");
            col++;
        }
    }

    /**
     * Get tuvs of source page from database. Return a list
     * 
     * @param p_sourcePage
     *            source page
     * @throws Exception
     */
    private List getPageTuvs(SourcePage p_sourcePage) throws Exception
    {
        return new ArrayList(ServerProxy.getTuvManager()
                .getSourceTuvsForStatistics(p_sourcePage));
    }

    /**
     * Get tuvs of target page from database. Return a list
     * 
     * @param p_targetPage
     *            target page
     * @throws Exception
     */
    private List getPageTuvs(TargetPage p_targetPage) throws Exception
    {
        return new ArrayList(ServerProxy.getTuvManager()
                .getTargetTuvsForStatistics(p_targetPage));
    }

    /**
     * Check the job data for report, for example "company name". If the job
     * data is correct, then return true.
     */
    protected boolean checkJob(Job p_job)
    {
        if (p_job == null)
            return false;

        if (CompanyWrapper.isSuperCompanyName(m_companyName))
            return true;

        String companyId = CompanyWrapper.getCompanyIdByName(m_companyName);
        if (companyId != null && companyId.equals(p_job.getCompanyId()))
            return true;

        return false;
    }

    @Override
    public String getReportType()
    {
        return ReportConstants.TRANSLATIONS_EDIT_REPORT;
    }

    @Override
    public void setPercent(int p_finishedJobNum)
    {
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public void cancel()
    {
        cancel = true;
    }

    private Sheet getSheet(Workbook p_workbook, int index)
    {
        Sheet sheet = p_workbook.getSheetAt(index);
        if (sheet == null)
            sheet = p_workbook.createSheet();
        return sheet;
    }

    private Row getRow(Sheet p_sheet, int p_col)
    {
        Row row = p_sheet.getRow(p_col);
        if (row == null)
            row = p_sheet.createRow(p_col);
        return row;
    }

    private Cell getCell(Row p_row, int index)
    {
        Cell cell = p_row.getCell(index);
        if (cell == null)
            cell = p_row.createCell(index);
        return cell;
    }

    /**
     * Create workbook name areas for category failure drop down list, it is
     * from "AA8" to "AAn".
     * <P>
     * Only write the data of drop down list into the first sheet as it can be
     * referenced from all sheets.
     * </P>
     * <P>
     * The formula is like
     * "[sheetName]!$AA$[startRow]:$AA$[endRow]",i.e."TER!$AA$8:$AA$32".
     * </P>
     */
    private void createCategoryFailureNameArea(Workbook p_workbook)
    {
        try
        {
            // Ensure the name area is written only one time,otherwise it has
            // problem when open generated excel file.
            if (p_workbook.getNumberOfSheets() == 1)
            {
                Sheet firstSheet = getSheet(p_workbook, 0);
                List<String> categories = getFailureCategoriesList();
                // Set the categories in "AA" column, starts with row 8.
                int col = 26;
                for (int i = 0; i < categories.size(); i++)
                {
                    Row row = getRow(firstSheet, SEGMENT_START_ROW + i);
                    Cell cell = getCell(row, col);
                    cell.setCellValue(categories.get(i));
                }

                String formula = firstSheet.getSheetName() + "!$AA$"
                        + (SEGMENT_START_ROW + 1) + ":$AA$"
                        + (SEGMENT_START_ROW + categories.size());
                Name name = p_workbook.createName();
                name.setRefersToFormula(formula);
                name.setNameName(CATEGORY_FAILURE_DROP_DOWN_LIST);

                // Hide "AA" column
                firstSheet.setColumnHidden(26, true);
            }
        }
        catch (Exception e)
        {
            logger.error(
                    "Error when create hidden area for category failures.", e);
        }
    }

    private String[] getCommentStatusList(String lastComment)
    {
        List<String> status = new ArrayList<String>();
        if (lastComment == null || "".equals(lastComment))
        {
            status.add(Issue.STATUS_QUERY);
        }
        else
        {
            status.addAll(IssueOptions.getAllStatus());
        }

        String[] statusArray = new String[status.size()];
        status.toArray(statusArray);
        
        return statusArray;
    }

    private void addCommentStatusValidation(Sheet p_sheet,
            String[] statusArray, int startRow, int lastRow, int startCol,
            int lastCol)
    {
        DataValidationHelper dvHelper = p_sheet.getDataValidationHelper();
        DataValidationConstraint dvConstraint = dvHelper
                .createExplicitListConstraint(statusArray);
        CellRangeAddressList addressList = new CellRangeAddressList(startRow,
                lastRow, startCol, lastCol);
        DataValidation validation = dvHelper.createValidation(dvConstraint,
                addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        p_sheet.addValidationData(validation);
    }

    /**
     * Add category failure drop down list. It is from "L8" to "Ln".
     * 
     * @param p_sheet
     * @param startRow
     * @param lastRow
     * @param startColumn
     * @param lastColumn
     */
    private void addCategoryFailureValidation(Sheet p_sheet, int startRow,
            int lastRow, int startColumn, int lastColumn)
    {
        // Add category failure drop down list here.
        DataValidationHelper dvHelper = p_sheet.getDataValidationHelper();
        DataValidationConstraint dvConstraint = dvHelper
                .createFormulaListConstraint(CATEGORY_FAILURE_DROP_DOWN_LIST);
        CellRangeAddressList addressList = new CellRangeAddressList(startRow,
                lastRow, startColumn, lastColumn);
        DataValidation validation = dvHelper.createValidation(dvConstraint,
                addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        p_sheet.addValidationData(validation);
    }
}
