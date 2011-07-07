<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	isErrorPage="true"
	import="org.opennms.web.category.*"
%>

<%
    CategoryNotFoundException cnfe = null;
    
    if( exception instanceof CategoryNotFoundException ) {
        cnfe = (CategoryNotFoundException)exception;
    } else if( exception instanceof ServletException ) {
        cnfe = (CategoryNotFoundException)((ServletException)exception).getRootCause();
    } else {
        throw new ServletException( "This error page does not handle this exception type.", exception );
    }    
    
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Error" />
  <jsp:param name="headTitle" value="Category Not Found" />
  <jsp:param name="headTitle" value="Error" />
  <jsp:param name="breadcrumb" value="Error" />
</jsp:include>


<h1>Category Not Found</h1>

<p>
  No information is currently available for the
  <strong><%=cnfe.getCategory()%></strong> category.
  Either the category does not exist, or its information is still being
  calculated.  
</p>

<jsp:include page="/includes/footer.jsp" flush="false" />
