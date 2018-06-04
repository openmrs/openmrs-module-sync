<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/history.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />
<openmrs:htmlInclude file="/moduleResources/sync/jquery.dataTables.min.css" />
<openmrs:htmlInclude file="/moduleResources/sync/jquery.dataTables.min.js" />

<script type="text/javascript">
    $j(document).ready(function() {
        $j('#transmission-logs-table').DataTable();
    });
</script>

<h2><spring:message code="sync.transmissionHistory.title"/></h2>

<b class="boxHeader">
	<table>
		<tr>
			<td><spring:message code="sync.changes.all"/></td>
		</tr>
	</table>
</b>
<div class="box">
  <table id="transmission-logs-table" class="hover stripe row-border">
	  <thead>
	  	<tr>
			<th><spring:message code="sync.config.server.name" /></th>
			<th><spring:message code="sync.config.server.lastSync.datetime" /></th>
			<th><spring:message code="sync.transmission.run.by" /></th>
			<th><spring:message code="sync.transmission.error.message" /></th>
			<th><spring:message code="sync.transmission.error.details"/></th>
		</tr>
	  </thead>
	  <tbody>
		<c:if test="${not empty transmissionLogs}">
			<c:forEach var="transmissionLog" items="${transmissionLogs}" varStatus="status">
				<tr>
					<td>${transmissionLog.remoteServer.nickname}</td>
					<td><openmrs:formatDate date="${transmissionLog.runAt}" format="${syncDateDisplayFormat}" /></td>
					<td>${transmissionLog.runBy.username}</td>
					<td>${transmissionLog.errorMessage}</td>
					<td>${transmissionLog.detailedError}</td>
				</tr>
			</c:forEach>
		</c:if>
	  </tbody>
  </table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>