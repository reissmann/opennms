/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.utils.BeanUtils;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.NotificationDao;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.netmgt.dao.filter.Filter;
import org.opennms.netmgt.dao.filter.notification.AcknowledgedByFilter;
import org.opennms.netmgt.dao.filter.notification.NotificationCriteria;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath:/daoWebRepositoryTestContext.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class DaoWebNotificationRepositoryTest implements InitializingBean {

    @Autowired
    DatabasePopulator m_dbPopulator;
    
    @Autowired
    WebNotificationRepository m_daoNotificationRepo;
    
    @Before
    public void setUp(){
        m_dbPopulator.populateDatabase();
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }
    
    @Test
    @Transactional
    public void testNotificationCount(){
        List<Filter> filterList = new ArrayList<Filter>();
        Filter[] filters = filterList.toArray(new Filter[0]);
        AcknowledgeType ackType = AcknowledgeType.UNACKNOWLEDGED;
        int notificationCount = m_daoNotificationRepo.countMatchingNotifications(new NotificationCriteria(ackType, filters));
        assertEquals(1, notificationCount);
    }

    @Test
    @Transactional
    public void testGetMatchingNotifications() {
        List<Filter> filterList = new ArrayList<Filter>();
        int limit = 10;
        int multiple = 0;
        AcknowledgeType ackType = AcknowledgeType.UNACKNOWLEDGED;
        NotificationDao.SortStyle sortStyle = NotificationDao.SortStyle.DEFAULT_SORT_STYLE;
        Filter[] filters = filterList.toArray(new Filter[0]);
        Notification[] notices = m_daoNotificationRepo.getMatchingNotifications(new NotificationCriteria(filters, sortStyle, ackType, limit, limit * multiple));
        assertEquals(1, notices.length);
        assertEquals("This is a test notification", notices[0].getTextMessage());
    }

    @Test
    @JUnitTemporaryDatabase // Relies on specific IDs so we need a fresh database
    public void testGetNotification(){
        Notification notice = m_daoNotificationRepo.getNotification(1);
        assertNotNull(notice);
    }
    
    @Test
    @Transactional
    public void testAcknowledgeNotification(){
        m_daoNotificationRepo.acknowledgeMatchingNotification("TestUser", new Date(), new NotificationCriteria());
        
        int notifCount = m_daoNotificationRepo.countMatchingNotifications(new NotificationCriteria(new AcknowledgedByFilter("TestUser")));
        assertEquals(1, notifCount);
        
        Notification[] notif = m_daoNotificationRepo.getMatchingNotifications(new NotificationCriteria(new AcknowledgedByFilter("TestUser")));
        assertEquals(1, notif.length);
        assertEquals("TestUser", notif[0].m_responder);
    }
}
