/**
 * Copyright (c) 2014, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.ucla.wise.initializer;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;

import edu.ucla.wise.commons.AdminApplication;
import edu.ucla.wise.commons.SurveyorApplication;
import edu.ucla.wise.emailscheduler.EmailScheduler;

/**
 * WiseApplicationInitializer class is used to initialize the classes needed for
 * running the WISE Application.
 * 
 * Things to consider before running WISE on server:
 * 
 * 1. Check if the properties file is correct 2. Check if the configuration is
 * for development or for production
 * 
 * @author Pralav
 * @version 1.0
 */
public class WiseApplicationInitializer implements ServletContextListener {

    public static final String WISE_HOME = "WISE_HOME";

    public static final Logger LOGGER = Logger.getLogger(WiseApplicationInitializer.class);

    /**
     * Destroys the email scheduler.
     * 
     * @param arg0
     *            ServletContextEvent.
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        EmailScheduler.destroyScheduler();
    }

    /**
     * Initializes all the needed classes and starts the email scheduler thread.
     * 
     * @param servletContextEvent
     *            ServletContextEvent.
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            LOGGER.info("Wise Application initializing");

            String wiseHome = System.getenv(WISE_HOME);
            if (Strings.isNullOrEmpty(wiseHome)) {
                LOGGER.info("WISE_HOME environment variable is not set");
                return;
            }

            String rootFolderPath = servletContextEvent.getServletContext().getRealPath("/");
            WiseProperties properties = new WiseProperties(wiseHome + "wise.properties", "WISE");
            String contextPath = servletContextEvent.getServletContext().getContextPath();

            WiseConfiguration configuration = new ProductionConfiguration(properties);

            // Wait for WiseStudySpaceWizard to complete loading
            Thread.sleep(10000);

            // All initializing statements below
            this.initializeStudySpaceParametersProvider(configuration);
            this.initializeAdminApplication(contextPath, properties);
            this.initializeSurveyApplication(contextPath, rootFolderPath, properties);
            this.startEmailSendingThreads(properties, configuration);
            // end of initializing statements

            LOGGER.info("Wise Application initialized");
        } catch (IOException e) {
            LOGGER.error("IO Exception while initializing", e);
        } catch (IllegalStateException e) {
            LOGGER.error("The admin or the survey app was not " + "initialized, WISE application cannot start", e);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for StudySpaceWizard to load", e);
        }

    }

    private void initializeStudySpaceParametersProvider(WiseConfiguration config) {
        StudySpaceParametersProvider.initialize(config);
    }

    private void initializeAdminApplication(String contextPath, WiseProperties properties) throws IOException {
        AdminApplication.initialize(contextPath, properties);
    }

    private void initializeSurveyApplication(String contextPath, String rootFolderPath, WiseProperties properties)
            throws IOException {
        SurveyorApplication.initialize(contextPath, rootFolderPath, properties);
    }

    private void startEmailSendingThreads(WiseProperties properties, WiseConfiguration configuration) {
        if (configuration.getConfigType() == WiseConfiguration.CONFIG_TYPE.PRODUCTION) {
            LOGGER.info("Staring Email Scheduler");
            EmailScheduler.intialize(properties);
            EmailScheduler.getInstance().startEmailSendingThreads();
            LOGGER.info("Email Scheduler is alive");
        } else {
            LOGGER.info("Skipping email scheduler in dev mode");
        }
    }
}