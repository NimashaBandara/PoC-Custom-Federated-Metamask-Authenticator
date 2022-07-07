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

import java.util.Arrays;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import java.io.*;
import java.util.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.common.model.Property;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MetamaskAuthenticator extends AbstractApplicationAuthenticator
        implements FederatedApplicationAuthenticator {

    @Override
    public boolean canHandle(HttpServletRequest request) {

        return MetamaskAuthenticationConstants.LOGIN_TYPE.equals(getLoginType(request));
    }

    @Override
    public String getFriendlyName() {

        return MetamaskAuthenticationConstants.FRIENDLY_NAME;
    }

    @Override
    public String getName() {

        return MetamaskAuthenticationConstants.NAME;
    }

    @Override
    public List<Property> getConfigurationProperties() {
        List<Property> configProperties = new ArrayList<>();
        Property callbackUrl = new Property();
        callbackUrl.setDisplayName("Callback URL");
        callbackUrl.setName(MetamaskAuthenticationConstants.CALLBACK_URL);
        callbackUrl.setDescription("The callback URL should be common-auth url.");
        callbackUrl.setDisplayOrder(3);
        configProperties.add(callbackUrl);

        Property tenantUrl = new Property();
        tenantUrl.setDisplayName("Tenant URL");
        tenantUrl.setName(MetamaskAuthenticationConstants.TENANT_URL);
        tenantUrl.setDescription(
                "Include hosted URL of your Identity Server tenant. ex: https://localhost:9443/t/{tenant-domain}");
        tenantUrl.setDisplayOrder(3);
        configProperties.add(tenantUrl);

        return configProperties;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
            AuthenticationContext context) throws AuthenticationFailedException {

        try {
            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
            if (authenticatorProperties != null) {

                String authorizationEP = authenticatorProperties.get(MetamaskAuthenticationConstants.TENANT_URL)
                        + MetamaskAuthenticationConstants.OAUTH2_AUTH_URL;
                String callBackUrl = authenticatorProperties.get(MetamaskAuthenticationConstants.CALLBACK_URL);
                String state = context.getContextIdentifier() + "," + MetamaskAuthenticationConstants.LOGIN_TYPE;

                OAuthClientRequest authRequest = OAuthClientRequest.authorizationLocation(authorizationEP)
                        .setRedirectURI(callBackUrl)
                        .setState(state).buildQueryMessage();

                // redirect user to metamask.jsp login page
                String loginPage = authRequest.getLocationUri();
                response.sendRedirect(loginPage);
            } else {
                throw new AuthenticationFailedException("Error while retrieving properties. " +
                        "Authenticator Properties cannot be null");
            }
        } catch (OAuthSystemException | IOException e) {
            throw new AuthenticationFailedException("Exception while building authorization code request", e);
        }
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
            AuthenticationContext context) throws AuthenticationFailedException {
        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

        String address = request.getParameter("address");
        String signature = request.getParameter("signature");
        String userId = "";
        boolean isUserAvailable = false;
        boolean validation = false;
        String message = "sign this message wso2";
        String InputString = "";
        JSONObject resultsJObject = null;
        try {
            validation = validateMetamaskMessageSignature(address, message, signature);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (authenticatorProperties != null) {
            if (validation) {
                String inputUrl = authenticatorProperties.get(MetamaskAuthenticationConstants.TENANT_URL)
                        + MetamaskAuthenticationConstants.SEARCH_USER;
                // get the existing user
                InputString = "{\"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:SearchRequest\"],\"attributes\": [ \"userName\",\"country\"],\"filter\":\"addresses EQ "
                        +
                        address + "\",\"domain\":\"PRIMARY\",\"startIndex\": 1,\"count\": 10}";
                resultsJObject = callSCIM(inputUrl, InputString);
                int tot = resultsJObject.getInt("totalResults");
                if (tot != 0) {
                    JSONArray resultsJArray1 = resultsJObject.optJSONArray("Resources");
                    JSONObject firstData = resultsJArray1.optJSONObject(0);
                    userId = firstData.getString("id");
                    isUserAvailable = true;
                } else {
                    isUserAvailable = false;
                }

                // create user if user is not available
                if (!isUserAvailable) {
                    inputUrl = authenticatorProperties.get(MetamaskAuthenticationConstants.TENANT_URL)
                            + MetamaskAuthenticationConstants.CREATE_USER;
                    String generatedString = RandomStringUtils.randomAlphabetic(10);
                    InputString = "{\"schemas\":[],\"userName\":\"" + generatedString
                            + "\",\"password\":\"wso2metamask\",\"addresses\":[\""
                            + address + "\"]}";
                    resultsJObject = callSCIM(inputUrl, InputString);
                    String id = resultsJObject.getString("id");
                    userId = id;
                }
                AuthenticatedUser authenticatedUser = AuthenticatedUser
                        .createFederateAuthenticatedUserFromSubjectIdentifier(userId);
                context.setSubject(authenticatedUser);
            } else {
                System.out.println("Invalid Signature");
            }
        } else {
            throw new AuthenticationFailedException("Error while retrieving properties. " +
                    "Authenticator Properties cannot be null");
        }

    }

    // validate metamask signature
    private static boolean validateMetamaskMessageSignature(String address, String message, String signature)
            throws Exception {
        final String personalMessagePrefix = MetamaskAuthenticationConstants.PERSONAL_PREFIX;
        boolean match = false;
        final String prefix = personalMessagePrefix + message.length();
        final byte[] msgHash = Hash.sha3((prefix + message).getBytes());
        final byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }
        final Sign.SignatureData sd = new Sign.SignatureData(v,
                Arrays.copyOfRange(signatureBytes, 0, 32),
                Arrays.copyOfRange(signatureBytes, 32, 64));
        String addressRecovered = null;

        final BigInteger publicKey = Sign.recoverFromSignature(v - 27, new ECDSASignature(
                new BigInteger(1, sd.getR()),
                new BigInteger(1, sd.getS())), msgHash);
        if (publicKey != null) {
            addressRecovered = "0x" + Keys.getAddress(publicKey);
            if (addressRecovered.equals(address)) {
                match = true;
            }
        }
        return match;

    }

    private static JSONObject callSCIM(String urlString, String InputString) {
        StringBuilder res = new StringBuilder();
        JSONObject resultsJObject = null;
        try {
            URL url = new URL(urlString);
            String encoding = Base64.getEncoder().encodeToString(("admin:admin").getBytes("UTF-8"));
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Basic " + encoding);
            String jsonInputString = InputString;
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {

                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    res.append(responseLine.trim());
                }
                resultsJObject = new JSONObject(res.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultsJObject;
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        String state = request.getParameter(MetamaskAuthenticationConstants.OAUTH2_PARAM_STATE);
        if (state != null) {
            return state.split(",")[0];
        } else {
            return null;
        }
    }

    private String getLoginType(HttpServletRequest request) {

        String state = request.getParameter(MetamaskAuthenticationConstants.OAUTH2_PARAM_STATE);
        if (state != null) {
            String[] stateElements = state.split(",");
            if (stateElements.length > 1) {
                return stateElements[1];
            }
        }
        return null;
    }

}
