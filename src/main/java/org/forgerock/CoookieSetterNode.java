/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */


package org.forgerock;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.net.URLEncoder;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;


@Node.Metadata(outcomeProvider  = AbstractDecisionNode.OutcomeProvider.class,
               configClass      = CoookieSetterNode.Config.class)
public class CoookieSetterNode extends AbstractDecisionNode {


    private final Config config;
    private final Realm realm;
    private final static String DEBUG_FILE = "CookieSetterNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private static final Duration EXPIRY_TIME_IN_HOURS = Duration.duration(5, HOURS);
    private final UUID nodeId;
    private String cookieValue = "";
    private String cookieValueEncoded = "";
    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100)
        default String cookieName() {
            return "My-Cookie";
        }

        @Attribute(order = 200)
        default boolean cookieFromString() {
            return false;
        }

        @Attribute(order = 300)
        default String cookieStringValue() {
            return "demo";
        }

        @Attribute(order = 400)
        default boolean cookieFromSharedState() {
            return false;
        }

        @Attribute(order = 500)
        default String cookieSharedStateValue() {
            return "none";
        }

        ;

        @Attribute(order = 600)
        default boolean encodeCookieValue() {
            return false;
        }

        @Attribute(order = 700)
        @org.forgerock.openam.sm.annotations.adapters.TimeUnit(HOURS)
        default Duration maxLife() {
            return EXPIRY_TIME_IN_HOURS;
        }

        /**
         * If true, instructs the browser to only send the cookie on secure connections.
         *
         * @return true to use secure cookie.
         */
        @Attribute(order = 800)
        default boolean useSecureCookie() {
            return false;
        }

        /**
         * If true, instructs the browser to prevent access to this cookie and only use it for http.
         *
         * @return true to use http only cookie.
         */
        @Attribute(order = 900)
        default boolean useHttpOnlyCookie() {
            return false;
        }

    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm The realm the node is in.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public CoookieSetterNode(@Assisted Config config, @Assisted Realm realm, @Assisted UUID nodeId) throws NodeProcessException {
        this.config = config;
        this.realm = realm;
        this.nodeId = nodeId;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        debug.error("[" + DEBUG_FILE + "]: Cookie Setter Node Started");
        Action.ActionBuilder actionBuilder = goTo(true);
        try {
            if (config.cookieFromString() && !config.cookieFromSharedState()) {
                cookieValue = config.cookieStringValue();
            } else if (!config.cookieFromString() && config.cookieFromSharedState()) {
                cookieValue = context.sharedState.get(config.cookieSharedStateValue()).asString();
            } else if (config.cookieFromString() && config.cookieFromSharedState()) {
                cookieValue = config.cookieStringValue() + " " + context.sharedState.get(config.cookieSharedStateValue()).asString();
            }

            if (config.encodeCookieValue()) {
                cookieValueEncoded = URLEncoder.encode(cookieValue, "UTF-8");
                cookieValue = cookieValueEncoded;
            }

        } catch (Exception e) {
            debug.error("[" + DEBUG_FILE + "]: Something went wrong :-( " + e);
            throw new NodeProcessException(e);

        }

        debug.error("[" + DEBUG_FILE + "]: Cookie Value set to: " + cookieValue);

        try {
            actionBuilder.putSessionProperty("customCookie", config.cookieName());
            actionBuilder.putSessionProperty("cookieValue", cookieValue);
            actionBuilder.addSessionHook(CookieTreeHook.class, nodeId, getClass().getSimpleName());
        } catch (Exception e) {
            debug.error("[" + DEBUG_FILE + "]: Something went wrong :-( " + e);
            actionBuilder = goTo(false);

        }
        return actionBuilder.build();
    }
}
