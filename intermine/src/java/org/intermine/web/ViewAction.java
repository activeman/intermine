package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.Globals;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Query;
import org.intermine.web.results.ChangeResultsForm;
import org.intermine.web.results.PagedResults;
import org.intermine.web.results.TableHelper;

/**
 * Action to handle buttons on view tile
 * @author Mark Woodbridge
 */
public class ViewAction extends Action
{
    /**
     * Run the query and forward to the results page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        ChangeResultsForm resultsForm =
            (ChangeResultsForm) session.getAttribute("changeResultsForm");
        if (resultsForm != null) {
            resultsForm.reset(mapping, request);
        }

        PagedResults pr;
        try {
            Query q = MainHelper.makeQuery(query, profile.getSavedBags());
            pr = TableHelper.makeTable(os, q, query.getView());
        } catch (ObjectStoreException e) {
            ActionErrors errors = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
            if (errors == null) {
                errors = new ActionErrors();
                request.setAttribute(Globals.ERROR_KEY, errors);
            }
            String key = (e instanceof ObjectStoreQueryDurationException)
                ? "errors.query.estimatetimetoolong"
                : "errors.query.objectstoreerror";
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(key));
            return mapping.findForward("query");
        }

        session.setAttribute(Constants.RESULTS_TABLE, pr);
        String queryName = SaveQueryHelper.findNewQueryName(profile.getSavedQueries());
        SaveQueryAction.saveQuery(request, queryName, query, pr.getResultsInfo());
        
        return mapping.findForward("results");
    }
}