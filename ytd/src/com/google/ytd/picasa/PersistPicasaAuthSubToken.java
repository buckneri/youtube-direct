/* Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ytd.picasa;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.client.http.AuthSubUtil;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ResourceNotFoundException;
import com.google.gdata.util.ServiceException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.ytd.dao.AdminConfigDao;
import com.google.ytd.model.AdminConfig;
import com.google.ytd.util.Util;

/**
 * AuthSub redirection flow to persist the token belonging to the admin Picasa
 * account.
 */
@Singleton
public class PersistPicasaAuthSubToken extends HttpServlet {
  private static final Logger log = Logger.getLogger(PersistPicasaAuthSubToken.class.getName());
  @Inject
  private Util util;
  @Inject
  private PersistenceManagerFactory pmf;
  @Inject
  private PicasaApiHelper picasaApi;
  @Inject
  private AdminConfigDao adminConfigDao;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PersistenceManager pm = pmf.getPersistenceManager();

    try {
      String token = AuthSubUtil.getTokenFromReply(req.getQueryString());
      token = URLDecoder.decode(token, "UTF-8");
      
      if (util.isNullOrEmpty(token)) {
        throw new IllegalArgumentException(String.format("Could not retrieve token from "
            + "AuthSub response. request.getQueryString() => %s", req.getQueryString()));
      }

      String sessionToken = AuthSubUtil.exchangeForSessionToken(token, null);

      picasaApi.setAuthSubToken(sessionToken);

      String picasaUsername = picasaApi.getCurrentUsername();
      if (util.isNullOrEmpty(picasaUsername)) {
        // TODO: Throw a better Exception class here.
        throw new IllegalArgumentException("Unable to retrieve a Picasa username for "
            + "the authenticated user.");
      }

      AdminConfig adminConfig = adminConfigDao.getAdminConfig();
      adminConfig.setPicasaAuthSubToken(sessionToken);
      adminConfig.setPicasaUsername(picasaUsername);

      pm.makePersistent(adminConfig);

      resp.sendRedirect(util.addNamespaceParamIfNeeded("/admin#configuration"));
    } catch (ResourceNotFoundException e) {
      log.log(Level.WARNING, "", e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No Picasa account found for the "
          + "authenticated user.");
    } catch (IllegalArgumentException e) {
      log.log(Level.WARNING, "", e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    } catch (AuthenticationException e) {
      log.log(Level.WARNING, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (GeneralSecurityException e) {
      log.log(Level.WARNING, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } finally {
      pm.close();
    }
  }
}