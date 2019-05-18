/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.session.Session;
import org.forgerock.util.time.TimeService;

import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@TreeHook.Metadata(configClass = CoookieSetterNode.Config.class)
public class CookieTreeHook implements TreeHook {
    private final Session session;
    private final Request request;
    private final Response response;
    private final CoookieSetterNode.Config config;
    private final static String DEBUG_FILE = "CookieSetterNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private final CookieResponseHandler cookieResponseHandler;

    @Inject
    public CookieTreeHook(@Assisted Session session, @Assisted Request request, @Assisted Response response,
                          @Assisted CoookieSetterNode.Config config) {
        this.request = request;
        this.response = response;
        this.config = config;
        this.session = session;
        this.cookieResponseHandler = InjectorHolder.getInstance(CookieResponseHandler.class);

    }

    @Override
    public void accept() throws TreeHookException {
        String cookieValue = "";
        debug.error("[" + DEBUG_FILE + "]: Cookie Tree Hook");
        try {
            debug.error("[" + DEBUG_FILE + "]: Cookie Value from Session " + session.getProperty("cookieValue") );
            cookieValue = session.getProperty("cookieValue");
        } catch (SessionException e) {
            e.printStackTrace();
        }
        long expiryInMillis = TimeService.SYSTEM.now() + config.maxLife().to(TimeUnit.MILLISECONDS);
                cookieResponseHandler.setCookieOnResponse(response, request, config.cookieName(), cookieValue, new Date(expiryInMillis), config.useSecureCookie(), config.useHttpOnlyCookie());
            }
        }

