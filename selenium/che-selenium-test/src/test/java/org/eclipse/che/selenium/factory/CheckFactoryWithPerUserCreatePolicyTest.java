/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.factory;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.selenium.core.TestGroup.FLAKY;
import static org.eclipse.che.selenium.core.TestGroup.UNDER_REPAIR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.google.inject.Inject;
import org.eclipse.che.api.factory.shared.dto.PoliciesDto;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.factory.FactoryTemplate;
import org.eclipse.che.selenium.core.factory.TestFactory;
import org.eclipse.che.selenium.core.factory.TestFactoryInitializer;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.eclipse.che.selenium.pageobject.NotificationsPopupPanel;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.openqa.selenium.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Mihail Kuznyetsov */
@Test(groups = {FLAKY, UNDER_REPAIR})
public class CheckFactoryWithPerUserCreatePolicyTest {
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Dashboard dashboard;
  @Inject private NotificationsPopupPanel notificationsPopupPanel;
  @Inject private TestFactoryInitializer testFactoryInitializer;
  @Inject private SeleniumWebDriver seleniumWebDriver;
  @Inject private SeleniumWebDriverHelper seleniumWebDriverHelper;
  private TestFactory testFactory;
  private final Logger LOG = LoggerFactory.getLogger(CheckFactoryWithPerUserCreatePolicyTest.class);

  @BeforeClass
  public void setUp() throws Exception {
    TestFactoryInitializer.TestFactoryBuilder factoryBuilder =
        testFactoryInitializer.fromTemplate(FactoryTemplate.MINIMAL);
    factoryBuilder.setPolicies(newDto(PoliciesDto.class).withCreate("perUser"));
    testFactory = factoryBuilder.build();
  }

  @AfterClass
  public void tearDown() throws Exception {
    testFactory.delete();
  }

  @Test
  public void checkFactoryAcceptingWithPerUserPolicy() throws Exception {
    dashboard.open();

    // accept factory
    testFactory.open(seleniumWebDriver);

    seleniumWebDriverHelper.switchToIdeFrameAndWaitAvailability();
    projectExplorer.waitProjectExplorer();

    try {
      notificationsPopupPanel.waitExpectedMessageOnProgressPanelAndClose("Project Spring imported");
    } catch (TimeoutException ex) {
      // remove try-catch block after issue has been resolved
      fail("Known random failure https://github.com/eclipse/che/issues/10728");
    }

    String workspaceUrl = seleniumWebDriver.getCurrentUrl();
    // accept factory
    testFactory.open(seleniumWebDriver);

    try {
      seleniumWebDriverHelper.switchToIdeFrameAndWaitAvailability();
    } catch (TimeoutException ex) {
      LOG.info(
          String.format(
              CheckFactoryWithPerUserCreatePolicyTest.class.getSimpleName(),
              testFactory.getWorkspaceStatusAssociatedWithFactory(),
              "Current workspace status for %s is: %s"));
      fail("Known permanent failure https://github.com/eclipse/che/issues/8798");
    }
    projectExplorer.waitProjectExplorer();

    // factory has been accepted in the same workspace
    assertEquals(workspaceUrl, seleniumWebDriver.getCurrentUrl());
  }
}
