/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.feature1_1;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwtMPConfigTests.MPConfigLocation;
import com.ibm.ws.security.mp.jwt21.fat.sharedTests.Feature12Enabled_GenericEnvVarsAndSystemPropertiesTests;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as system properties.
 * We'll test that mpJwt-1.2 mp config properties are not used when the mpJwt-1.1 feature is enabled.
 * Test class runs the same tests as the environment variable tests, so, we'll use a common test
 * class - we'll only use this class to request that the mp properties are set as properties
 * and give the test cases unique names.
 *
 **/

@SkipForRepeat({ SkipForRepeat.EE9_FEATURES + "_" + MPJwt12FatConstants.MP_JWT_21 }) // tests purposely skipped as we want the server.xml using 1.1 to prove that the function isn't accidentally allowed.
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Feature12Enabled_MpConfigAsSystemProperties extends Feature12Enabled_GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = Feature12Enabled_MpConfigAsSystemProperties.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(MPConfigLocation.SYSTEM_PROP.toString()));

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat.jvmOptions")
    public static LibertyServer sysPropsResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        // tell the common tests which server to use and where to put the mp config properties
        // use a server with a jvm.options file and set the mp config properties as system properties
        commonMpJwt21Setup(sysPropsResourceServer, MPConfigLocation.SYSTEM_PROP);

    }

}