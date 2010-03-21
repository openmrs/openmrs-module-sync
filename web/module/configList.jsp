<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/config.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<%@ include file="localHeader.jsp" %>

<style>
 .parent {
   font-weight: bold;
  }
</style>

<script language="JavaScript">
	function confirmDelete(id) {
		var isConfirmed = confirm("<spring:message code="sync.config.server.confirmDelete" />");
		if ( isConfirmed ) {
			document.getElementById("deleteServer" + id).submit();
		}
	}
</script>

<h2><spring:message code="sync.config.title"/></h2>

<div id="general">
	
	<div id="serverList">
		<b class="boxHeader"><spring:message code="sync.config.servers.remote"/></b>
		<div class="box">
			<table id="syncChangesTable" cellpadding="10" cellspacing="0">
				<c:if test="${not empty configListBackingObject.serverList}">
					<thead>
						<tr>
							<th></th>
							<th></th>
							<th></th>
							<th align="center" colspan="2" style="background-color: #eef3ff; text-align: center; font-weight: normal;"><spring:message code="sync.config.server.synchronize.manually" /></th>
							<th align="center" style="text-align: center; background-color: #fee; font-weight: normal;"><spring:message code="sync.config.server.synchronize.automatic" /></th>
							<th></th>
						</tr>
						<tr>
							<th><spring:message code="sync.config.server.name" /></th>
							<th style="text-align: center;"><spring:message code="sync.config.server.type" /></th>
							<th><spring:message code="sync.config.server.lastSync" /></th>
							<th style="background-color: #eef; text-align: center;"><img src="${pageContext.request.contextPath}/images/save.gif" border="0" style="margin-bottom: -3px;">
								<spring:message code="sync.config.server.syncViaFile" />
							<th style="background-color: #efe; text-align: center;"><img src="${pageContext.request.contextPath}/images/lookup.gif" border="0" style="margin-bottom: -3px;">
								<spring:message code="sync.config.server.syncViaWeb" />
							<th style="background-color: #fee; text-align: center;"><img src="${pageContext.request.contextPath}/moduleResources/sync/scheduled_send.gif" border="0" style="margin-bottom: -3px;">
								<spring:message code="sync.config.server.syncAutomatic" />
								(<spring:message code="sync.general.scheduled" />)
							<th style="text-align: center;"><spring:message code="sync.config.server.delete" /></th>
						</tr>
					</thead>
					<tbody id="globalPropsList">
						<c:set var="bgStyle" value="eee" />				
						<c:set var="bgStyleFile" value="dde" />				
						<c:set var="bgStyleWebMan" value="ded" />				
						<c:set var="bgStyleWebAuto" value="edd" />				
						<c:forEach var="server" items="${configListBackingObject.serverList}" varStatus="status">
							<tr>
								<td nowrap style="background-color: #${bgStyle};">
									<c:choose>
										<c:when test="${server.serverType == 'CHILD'}">
											<a href="configServer.form?serverId=${server.serverId}" disabled><b>${server.nickname}</b></a>
										</c:when>
										<c:otherwise>
											<a href="configServer.form?serverId=${server.serverId}"><b>${server.nickname}</b></a>
											<%--(${server.address})--%>
										</c:otherwise>
									</c:choose>
								</td>
								<td style="background-color: #${bgStyle}; text-align:center;"
									<c:if test="${server.serverType == 'PARENT'}">class="parent"</c:if>
									>
									${server.serverType}
								</td>
								<td style="background-color: #${bgStyle}; text-align:center;">
									<openmrs:formatDate date="${server.lastSync}" format="${syncDateDisplayFormat}" />
								</td>
								<td style="background-color: #${bgStyleFile}; text-align:center;">
									<c:choose>
										<c:when test="${server.serverType == 'CHILD'}">
											<a href="import.list?serverId=${server.serverId}">
												<spring:message code="sync.config.server.uploadAndReply" />
											</a>
										</c:when>
										<c:otherwise>
											<a href="status.list?mode=SEND_FILE">
												<spring:message code="sync.config.server.sendFile" />
											</a>
											&nbsp;
											<a href="status.list?mode=UPLOAD_REPLY">
												<spring:message code="sync.config.server.uploadResponse" />
											</a>
										</c:otherwise>
									</c:choose>
								</td>
								<td style="background-color: #${bgStyleWebMan}; text-align:center;">
									<c:choose>
										<c:when test="${server.serverType == 'CHILD'}">
											<span title="<spring:message code='sync.config.server.na.childWebSyncNotApplicable'/>"><spring:message code="sync.config.server.na"/></span>
										</c:when>
										<c:otherwise>
											<a href="status.list?mode=SEND_WEB">
												<spring:message code="sync.config.server.synchronizeNow" />
											</a>
										</c:otherwise>
									</c:choose>
								</td>
								<td style="background-color: #${bgStyleWebAuto}; text-align:center;">
									<c:choose>
										<c:when test="${server.serverType == 'CHILD'}">
											<span title="<spring:message code='sync.config.server.na.childWebSyncNotApplicable'/>"><spring:message code="sync.config.server.na"/></span>
										</c:when>
										<c:otherwise>
											<c:if test="${parentSchedule.started == false}">
												(<spring:message code="sync.config.parent.not.scheduled" />)
												<a href="configServer.form?serverId=${server.serverId}" style="font-size: 0.9em;">
													<spring:message code="sync.general.configure" />
												</a>
											</c:if>
											<c:if test="${parentSchedule.started == true}">
												<spring:message code="sync.config.parent.scheduled.every" />
												<b>${repeatInterval}</b>
												<spring:message code="sync.config.parent.scheduled.minutes" />
												<a href="configServer.form?serverId=${server.serverId}" style="font-size: 0.9em;">
													<spring:message code="sync.general.configure" />
												</a>
											</c:if>
										</c:otherwise>
									</c:choose>
								</td>
								<td style="background-color: #${bgStyle}; text-align:center;">
									<c:choose>
										<c:when test="${server.serverType != 'PARENT'}">
											<form id="deleteServer${server.serverId}" action="config.list" method="post">
												<input type="hidden" name="action" value="deleteServer" />
												<input type="hidden" id="serverId" name="serverId" value="${server.serverId}" />
												<a href="javascript:confirmDelete('${server.serverId}');"><img src="<%= request.getContextPath() %>/images/trash.gif" alt="delete" border="0" /></a>
											</form>
										</c:when>
										<c:otherwise>
											&nbsp;
										</c:otherwise>
									</c:choose>
								</td>								
							</tr>
							<c:choose>
								<c:when test="${bgStyle == 'eee'}">
									<c:set var="bgStyle" value="fff" />
									<c:set var="bgStyleFile" value="eef" />				
									<c:set var="bgStyleWebMan" value="efe" />				
									<c:set var="bgStyleWebAuto" value="fee" />				
								</c:when>
								<c:otherwise>
									<c:set var="bgStyle" value="eee" />
									<c:set var="bgStyleFile" value="dde" />				
									<c:set var="bgStyleWebMan" value="ded" />				
									<c:set var="bgStyleWebAuto" value="edd" />				
								</c:otherwise>
							</c:choose>
						</c:forEach>
					</c:if>
					<c:if test="${empty configListBackingObject.serverList}">
						<td colspan="3" align="left">
							<i><spring:message code="sync.config.servers.noItems" /></i>
						</td>
					</c:if>
					<tr>
						<td colspan="3">
							<br>
							<a href="configCurrentServer.form"><spring:message code="sync.config.server.config.current" /></a>
							|
							<a href="configServer.form?type=CHILD"><img src="${pageContext.request.contextPath}/images/add.gif" style="margin-bottom: -3px;" border="0" /></a>
							<a href="configServer.form?type=CHILD"><spring:message code="sync.config.server.config.child" /></a>
							<c:if test="${empty parent}">
								 |
								<a href="configServer.form?type=PARENT"><img src="${pageContext.request.contextPath}/images/add.gif" style="margin-bottom: -3px;" border="0" /></a>
								<a href="configServer.form?type=PARENT"><spring:message code="sync.config.server.config.parent" /></a>
							</c:if>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
