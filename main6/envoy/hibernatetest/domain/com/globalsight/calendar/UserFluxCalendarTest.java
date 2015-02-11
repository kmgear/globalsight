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

package com.globalsight.calendar;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import com.globalsight.persistence.hibernate.HibernateUtil;

public class UserFluxCalendarTest extends TestCase
{
	private String derek = "derek";
	
	public void testSave() throws Exception
	{
        int resultSize = listResult().size();

		UserFluxCalendar userCalendar = new UserFluxCalendar();

		userCalendar.setActivityBuffer(2);
		userCalendar.setLastUpdatedBy(derek);
		userCalendar.setLastUpdatedTime(new Date());
		userCalendar.setOwnerUserId(derek);
		userCalendar.setParentCalendarId(1);
		userCalendar.setTimeZoneId(derek);

		HibernateUtil.save(userCalendar);

        assertTrue(resultSize != listResult().size());
    }

	public void testFind() throws Exception
	{
		assertTrue(listResult().size() != 0);
	}

	public void testDelete() throws Exception
	{
		List result = listResult();
		HibernateUtil.delete(result);
		assertTrue(result.size() != listResult().size());
	}

    private List listResult()
    {
        String hql = "from UserFluxCalendar c where c.lastUpdatedBy = '" + derek + "'";
        return HibernateUtil.search(hql);
    }
}
