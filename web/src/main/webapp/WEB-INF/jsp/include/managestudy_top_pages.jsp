<%@ page contentType="text/html; charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<fmt:setBundle basename="org.akaza.openclinica.i18n.words" var="resword"/>
<fmt:setBundle basename="org.akaza.openclinica.i18n.workflow" var="resworkflow"/>
<fmt:setBundle basename="org.akaza.openclinica.i18n.format" var="resformat"/>

<jsp:useBean scope='session' id='userBean' class='org.akaza.openclinica.bean.login.UserAccountBean'/>
<jsp:useBean scope='session' id='study' class='org.akaza.openclinica.bean.managestudy.StudyBean' />
<jsp:useBean scope='session' id='userRole' class='org.akaza.openclinica.bean.login.StudyUserRoleBean' />
<jsp:useBean scope='request' id='isAdminServlet' class='java.lang.String' />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<meta http-equiv="content-type" content="text/html; charset=utf-8" />
	<meta http-equiv="X-UA-Compatible" content="IE=8" />

	<title><fmt:message key="openclinica" bundle="${resword}"/></title>

	<link rel="stylesheet" href="${pageContext.request.contextPath}/includes/styles.css" type="text/css" />

	<script type="text/JavaScript" src="${pageContext.request.contextPath}/includes/global_functions_javascript.js"></script>
	<script type="text/JavaScript" src="${pageContext.request.contextPath}/includes/Tabs.js"></script>
	<script type="text/JavaScript" src="${pageContext.request.contextPath}/includes/CalendarPopup.js"></script>
	<script type="text/JavaScript" src="${pageContext.request.contextPath}/includes/prototype.js"></script>
    <!-- Added for the new Calender -->
    <link rel="stylesheet" type="text/css" media="all" href="${pageContext.request.contextPath}/includes/new_cal/skins/aqua/theme.css" title="Aqua" />
    <script type="text/javascript" src="${pageContext.request.contextPath}/includes/new_cal/calendar.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/includes/new_cal/lang/calendar-en.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/includes/new_cal/lang/<fmt:message key="jscalendar_language_file" bundle="${resformat}"/>"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/includes/new_cal/calendar-setup.js"></script>
	<!-- End -->
</head>

<body class="main_BG"
	<c:choose>
		<c:when test="${tabId!= null && tabId>0}">
			onload="TabsForwardByNum(<c:out value="${tabId}"/>);<jsp:include page="../include/showPopUp2.jsp"/>"
		</c:when>
		<c:otherwise>
			<jsp:include page="../include/showPopUp.jsp"/>
		</c:otherwise>
	</c:choose>
> <!-- start of include/managestudy_top_pages.jsp -->
<table class="background"> 
	<tr>
		<td class="header_td"><script language="JavaScript">var StatusBoxValue=1;</script>
			<table class="header"><!-- Header Table -->
				<tr>
					<td class="header_td">
						<div class="logo"><img src="${pageContext.request.contextPath}/images/Logo.gif"></div>
						<!-- Main Navigation -->
						<jsp:include page="../include/navBar.jsp">
        					<jsp:param name="isSpringController" value="true" />
    					</jsp:include>
<!-- End Main Navigation -->
 <!-- end of include/managestudy_top_pages.jsp -->
 