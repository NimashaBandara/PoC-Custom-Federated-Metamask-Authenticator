# Custom Federated Metamask Authenticator

This authenticator is developed as a PoC to research conducted on "Integrating WSO2 Identity Server with a Decentralized IdP" as a internship project by WSO2 IAM team. The authenticator use Metamask digital wallet as decentralized federated authenticator.


### Set Up for the PoC

1. Download the latest pack of the identity server.(5.11.0.22 or onwards)
2. Create a local user claim for wallet address in the claims' section in management console(In this PoC the claim mapping was created as http://wso2.org/claims/wallet_address
   ) and map the claim with an existing attribute "addresses"(Still we do not have developed unique attribute in LDAP)
3. Add the uniqueness feature to the claim by dding additional property isUnique=true. ( follow https://is.docs.wso2.com/en/latest/learn/configuring-uniqueness-of-claims/ )
4. Download and create metamask wallet account. visit https://metamask.io/
5. Add metamask.jsp file into <IS_HOME>-> repository -> deployment -> server -> webapps -> authenticationendpoint folder
6. Add <servlet-mapping> and  <servlet> for metamask.jsp page to <IS_HOME>-> repository -> deployment -> server -> webapps -> authenticationendpoint -> web.xml
7. Build custom federated metamask authenticator jar with " mvn clean install "
8. Re-start Identity Server
9. Create metamask identity provider. management console -> Identity Provider -> add -> Federated authenticators -> Metamask Configuration. Provide "https://localhost:9443/commonauth" as callback url. 
10. Select created identity provider in Local & Outbound Authentication configuration of your service provider.

mappings in web. xml look like below:
``` 
<servlet>
<servlet-name>metamask.do</servlet-name>
<jsp-file>/metamask.jsp</jsp-file>
</servlet>
<servlet-mapping>
<servlet-name>metamask.do</servlet-name>
<url-pattern>/metamask.do</url-pattern>
</servlet-mapping>
```
