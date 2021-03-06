/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

/**
 * Sets a cookie on a Response. The format and parameters are specific to http set-cookie protocols.
 */
final class CookieResponseHandler {

    private static final String SET_COOKIE_HEADER_KEY = "Set-Cookie";
    private static final String DEFAULT_PATH = "/";

    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "CookieSetterNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    @Inject
    private CookieResponseHandler(CoreWrapper coreWrapper) {
        this.coreWrapper = coreWrapper;
    }

    void setCookieOnResponse(Response response, Request request, String cookieName, String cookieValue,
                             Date expiryDate, boolean isSecure, boolean isHttpOnly) {
        debug.error("[" + DEBUG_FILE + "]: CookieResponder - setCookieOnResponse");
        Collection<String> domains = coreWrapper.getCookieDomainsForRequest(request);
        if (!domains.isEmpty()) {
            debug.error("[" + DEBUG_FILE + "]: CookieResponder - domains is not empty");

            for (String domain : domains) {
                Cookie cookie = createCookie(cookieName, cookieValue, domain, expiryDate, isSecure, isHttpOnly);
                SetCookieHeader header = new SetCookieHeader(Collections.singletonList(cookie));
                for (String headerValue : header.getValues()) {
                    response.getHeaders().put(SET_COOKIE_HEADER_KEY, headerValue);
                }
            }
        } else {
            Cookie cookie = createCookie(cookieName, cookieValue, null, expiryDate, isSecure, isHttpOnly);
            response.getHeaders().put(SET_COOKIE_HEADER_KEY, cookie);
        }
    }

    private Cookie createCookie(String name, String value, String domain, Date expiryDate, boolean isSecure,
                                boolean isHttpOnly) {
        return new Cookie()
            .setName(name)
            .setValue(value)
            .setPath(DEFAULT_PATH)
            .setDomain(domain)
            .setExpires(expiryDate)
            .setSecure(isSecure)
            .setHttpOnly(isHttpOnly);
    }

    /**
     * Gets the org name from the response.
     *
     * @param response the response.
     * @return the org name.
     */
    static String getOrgName(Response response) {
        String realm = null;
        try {
            JsonValue jsonValues = JsonValue.json(response.getEntity().getJson());
            realm = jsonValues.get(REALM).asString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DNMapper.orgNameToDN(realm);
    }
}
