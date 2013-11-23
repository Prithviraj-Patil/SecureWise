package edu.ucla.wise.client;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.ucla.wise.commons.StudySpace;
import edu.ucla.wise.commons.SurveyorApplication;
import edu.ucla.wise.commons.User;
import edu.ucla.wise.commons.WISEApplication;
import edu.ucla.wise.commons.WiseConstants;

/**
 * DeclineNOWServlet class is used if user declined the invitation from the email link, then
 * forward him to the decline reason page.
 * 
 * @author Douglas Bell
 * @version 1.0  
 */
public class DeclineNOWServlet extends HttpServlet {
    static final long serialVersionUID = 1000;
    
    /**
     * Forwards the page to decline page after checking all the necessary parameter.
     * 
     * @param 	req	 HTTP Request.
     * @param 	res	 HTTP Response.
     * @throws 	ServletException and IOException. 
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res)
    		throws ServletException, IOException {
		
    	/* prepare for writing */
		PrintWriter out;
		res.setContentType("text/html");
		out = res.getWriter();
	
		HttpSession session = req.getSession(true);
		
		//Surveyor_Application s = (Surveyor_Application) session
		//	.getAttribute("SurveyorInst");
	
		/* get the ecoded study space ID */
		String spaceIdEncode = req.getParameter("t");
		
		/* get the email message ID */
		String msgIdEncode = req.getParameter("m");
	
		/* if can't get sufficient information, then the email URL maybe broken into lines */
		if (msgIdEncode == null || msgIdEncode.equalsIgnoreCase("")
				|| spaceIdEncode == null
				|| spaceIdEncode.equalsIgnoreCase("")) {
		    res.sendRedirect(SurveyorApplication.sharedFileUrl + "link_error"
		    		+ SurveyorApplication.htmlExt);
		    return;
		}
	
		/* decode the message ID & study space ID */
		String spaceId = WISEApplication.decode(spaceIdEncode);
		String msgId = WISEApplication.decode(msgIdEncode);
	
		/* initiate the study space ID and put it into the session */
		StudySpace theStudy = StudySpace.getSpace(spaceId);
		User theUser = theStudy == null ? null : theStudy.getUser(msgId);
	
		/* if the user can't be created, send error info */
		if (theUser == null) {
		    out.println("<HTML><HEAD><TITLE>Begin Page</TITLE>");
		    out.println("<LINK href='" + SurveyorApplication.sharedFileUrl
			    + "style.css' type=text/css rel=stylesheet>");
		    out.println("<body><center><table>");
		    // out.println("<body text=#000000 bgColor=#ffffcc><center><table>");
		    out.println("<tr><td>Error: Can't get the user information.</td></tr>");
		    out.println("</table></center></body></html>");
		    WISEApplication.logError(
			    "WISE BEGIN - Error: Can't create the user.", null);
		    return;
		}
	
		/* put the user into the session */
		session.setAttribute("USER", theUser);
	
		/* record this visit */
		theUser.recordDeclineHit(msgId, spaceId);
	
		/* mark user as declining */
		theUser.decline();
	
		/* forward to ask for reason of declining */
		String url = WISEApplication.rootURL + "/WISE/" + WiseConstants.SURVEY_APP
				+ "/decline" + SurveyorApplication.htmlExt;
		res.sendRedirect(url);
		out.close();
    }
}