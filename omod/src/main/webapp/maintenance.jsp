<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/maintenance.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp"%>
<style>
 .descriptionBox {
 	border-color: transparent;
 	border-width: 1px;
 	overflow-y: auto;
 	background-color: transparent;
 	padding: 1px;
 	height: 2.7em;
 }
 td.description {
 	padding-top: 0px;
 }
 #buttonsAtBottom {
 	padding: 5px;
 }
</style>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/engine.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />

<h2><spring:message code="sync.maintenance.title"/></h2>

<script language="JavaScript">
	function showHideDiv(id) {
		var div = document.getElementById(id);
		if ( div ) {
			if ( div.style.display != "none" ) {
				div.style.display = "none";
			} else { 
				div.style.display = "";
			}
		}
	}
	
	function hideDiv(id){
		var div = document.getElementById(id);
		div.style.display = "none";
	}
	
	function showDiv(id){
		var div = document.getElementById(id);
		div.style.display = "";
	}
	function enableDiv(id){
		var div = document.getElementById(id);
		div.disabled = false;
	}
	function disableDiv(id){
		var div = document.getElementById(id);
		div.disabled = true;
	}
	function showJournalArchiveResult(result){
		
		if(result){
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/accept.gif' border='0'>" + "&nbsp;" +"<span class='syncCOMMITTED'><b><spring:message code='sync.maintenance.archive.journal.success' /></b></span>");
		}
		else{
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/error.gif' border='0'>" + "&nbsp;" +"<span class='syncFAILED'><b><spring:message code='sync.maintenance.archive.journal.error'/></b></span>");
		}
		
		setTimeout("enableDiv('archiveJournalButton');DWRUtil.setValue('archiveResult','');", 4000);
			
	}
	
	function archiveSyncJournal(clearDir){
	disableDiv('archiveJournalButton');
	DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/loader.gif' border='0'>" + "&nbsp;" +"<span class='syncNEUTRAL'><b><spring:message code='sync.maintenance.archive.journal.progress' /></b></span>");
		DWRSyncService.archiveSyncJournal(clearDir,showJournalArchiveResult);
	}
	
	function showImportArchiveResult(result){
		if(result){
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/accept.gif' border='0'>" + "&nbsp;" +"<span class='syncCOMMITTED'><b><spring:message code='sync.maintenance.archive.import.success' /></b></span>");
		}
		else{
			DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/error.gif' border='0'>" + "&nbsp;" +"<span class='syncFAILED'><b><spring:message code='sync.maintenance.archive.import.error' /></b></span>");
		}
		setTimeout("enableDiv('archiveImportButton');DWRUtil.setValue('archiveResult','');", 4000);
	}
	function archiveSyncImport(clearDir){
	disableDiv('archiveImportButton');
	DWRUtil.setValue("archiveResult","&nbsp;" + "<img src='${pageContext.request.contextPath}/images/loader.gif' border='0'>" + "&nbsp;" +"<span class='syncNEUTRAL'><b><spring:message code='sync.maintenance.archive.import.progress' /></b></span>");
		DWRSyncService.archiveSyncImport(clearDir,showImportArchiveResult);
	}
</script>
<b class="boxHeader"><spring:message code="sync.maintenance.search.title"/></b>
<div class="box">
<form action="" method="GET">
  <label><strong><spring:message code="sync.maintenance.keyword"/></strong>
  <input type="text" id="keyword" name="keyword" value="${keyword}">
  </label>
  <input type="submit" id="searchButton" value="Search">
</form>
<c:if test="${not empty synchronizationMaintenanceList}">
<div style="position: relative; border: 1px solid gray; margin: 10px; padding: 0px;">
<table width="100%" border="0" align="center" cellpadding="0"
	cellspacing="0">
	<tr bgcolor="#E1E4EA">
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.type" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.name" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.action" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.timestamp" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.status" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"><b><spring:message
			code="sync.records.retryCount" /></b></td>
		<td align="left" valign="middle" style="padding: 8px;"></td>

	</tr>
	
		<c:set var="bgs" value="EDEED2" />
		<c:forEach var="syncRecord" items="${synchronizationMaintenanceList}"
			varStatus="status">
			<tr class="syncTr" bgcolor="#${bgs}"
				onclick="location='viewrecord.form?uuid=${syncRecord.uuid}'"
				height="25">
				<td align="left" valign="middle" style="padding: 8px;"><b><a
					href="viewrecord.list?uuid=${syncRecord.uuid}">${recordTypes[syncRecord.uuid]}</a></b></td>
				<td align="left" valign="middle" style="padding: 8px;"><c:if
					test="${not empty recordText[syncRecord.uuid]}">${recordText[syncRecord.uuid]}</c:if>
				</td>
				<td align="left" valign="middle" style="padding: 8px;"><spring:message
					code="sync.item.state_${recordChangeType[syncRecord.uuid]}" /></td>
				<td align="left" valign="middle" style="padding: 8px;"><openmrs:formatDate
					date="${syncRecord.timestamp}" format="${syncDateDisplayFormat}" /></td>
				<td align="left" valign="middle" style="padding: 8px;" id="state_${syncRecord.uuid}"><span
					class="sync${syncRecord.state}"> <spring:message
					code="sync.record.state_${syncRecord.state}" /></span></td>
				<td valign="middle" style="padding: 8px;">${syncRecord.retryCount}</td>
				<td valign="middle" style="padding: 8px;"><span id="message_${syncRecord.uuid}"></span></td>
				<c:choose>
					<c:when test="${bgs == 'EDEED2'}">
						<c:set var="bgs" value="F7F7EA" />
					</c:when>
					<c:otherwise>
						<c:set var="bgs" value="EDEED2" />
					</c:otherwise>
				</c:choose>
			</tr>
		</c:forEach>
</table>
</div>
</c:if>
<c:if test="${maxPages > 1}">
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<td>&nbsp;</td>
			<td align="center" valign="middle"><span><spring:message
				code="sync.maintenance.goto" />:</span> <c:forEach var="p"
				begin="${1}" end="${maxPages}" step="${1}">
		 	|
			<c:choose>
					<span class="syncPageNum"> <c:when test="${p==currentPage}">
						<a href="?keyword=${keyword}&page=${p}" style="font-size: 18px">${p}</a>
					</c:when> </span>
					<c:otherwise>
						<span class="syncPageNum"><a href="?keyword=${keyword}&page=${p}">${p}</a></span>
					</c:otherwise>
			  </c:choose>
			</c:forEach></td>
			<td>&nbsp;</td>
		</tr>
	</table>
</c:if> 
<c:if test="${empty synchronizationMaintenanceList}">
	<table>
		<tr>
			<td align="center" valign="middle"><i><spring:message
				code="sync.maintenance.noItems" /> <strong>${keyword}</strong></i></td>
		</tr>
	</table>
</c:if>
</div>
<br/>
<b class="boxHeader"><spring:message code="sync.maintenance.archive.title"/></b>
<div class="box"><br/>
<span><spring:message code="sync.maintenance.archive.description" /></span>
	<ul>
		<li>
			<span>
				<spring:message code="sync.maintenance.archive.journal" />
				<input id="archiveJournalButton" type="button"  onclick="archiveSyncJournal(true);" value="<spring:message code="sync.maintenance.archive.now" />"/></span>
		</li>
		<br/>
		<li>
			<span><spring:message code="sync.maintenance.archive.import" />
			<input id="archiveImportButton" type="button"  onclick="archiveSyncImport(true);" value="<spring:message
			code="sync.maintenance.archive.now" />" /></span>
		</li>
	</ul>
	<br/>
<div id="archiveResult">

</div>
<br/>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
