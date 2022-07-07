/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.metamask.federated.authenticator;

public class MetamaskAuthenticationConstants {

    public static final String LOGIN_TYPE = "metamask";
    public static final String FRIENDLY_NAME = "metamask";
    public static final String NAME = "MetamaskAuthenticator";
    public static final String OAUTH2_PARAM_STATE = "state";
    public static final String CALLBACK_URL = "callbackUrl";
    public static final String OAUTH2_AUTH_URL = "/authenticationendpoint/metamask.do";
    public static final String SEARCH_USER = "/scim2/Users/.search";
    public static final String CREATE_USER = "/scim2/Users";
    public static final String PERSONAL_PREFIX = "\u0019Ethereum Signed Message:\n";
    public static final String TENANT_URL = "tenantUrl";
}