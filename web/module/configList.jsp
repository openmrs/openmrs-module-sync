<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/config.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />
<openmrs:htmlInclude file="/dwr/util.js" />

<%@ include file="localHeader.jsp" %>

<script language="JavaScript">

	//Called to disable content when sync is disabled
	function disableDIVs() {
		hideDiv('advanced');
		hideDiv('serverList');
	}

	function confirmDelete(id) {
		var isConfirmed = confirm("<spring:message code="sync.config.server.confirmDelete" />");
		if ( isConfirmed ) {
			document.getElementById("deleteServer" + id).submit();
		}
	}
</script>

<table>
	<tr>
		<td>
			<h2><spring:message code="sync.config.title"/></h2>
		</td>
		<td>
			&nbsp;&nbsp;
			<a href="javascript://" onclick="hideDiv('advanced');showDiv('general');"><spring:message code="sync.config.menu.general" /></a>
			|
		  	<a href="javascript://" onclick="hideDiv('general');showDiv('advanced');"><spring:message code="sync.config.menu.advanced" /></a>
		</td>
	</tr>
</table>

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
								<td style="background-color: #${bgStyle}; text-align:center;">
									<c:choose>
										<c:when test="${server.serverType == 'PARENT'}">
											<b>${server.serverType}</b>
										</c:when>
										<c:otherwise>
											${server.serverType}
										</c:otherwise>
									</c:choose>
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
											-
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
											-
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

<div id="advanced" style="display:none;">

	<form action="config.list" method="post">
	<input type="hidden" name="action" value="saveClasses" />

		<b class="boxHeader"><spring:message code="sync.config.advanced.configOptions"/></b>
		<div class="box">
			<table id="syncStatus" cellpadding="8" cellspacing="0">
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverUuid" /></b></td>
					<td><input type="text" size="50" name="serverUuid" id="serverUuid" value="${localServerUuid}" /></td>
					<td><spring:message code="sync.config.advanced.serverUuid.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverName" /></b></td>
					<td><input type="text" size="50" name="serverName" id="serverName" value="${localServerName}" /></td>
					<td><spring:message code="sync.config.advanced.serverName.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverAdminEmail" /></b></td>
					<td><input type="text" size="50" name="serverAdminEmail" id="serverAdminEmail" value="${localServerAdminEmail}" /></td>
					<td><spring:message code="sync.config.advanced.serverAdminEmail.info" /></td>
				</tr>
			</table>
		</div>
	
		<br />
	
		<b class="boxHeader"><spring:message code="sync.config.advanced.objects"/></b>
		<div class="box">
			<table>
				<tr>
					<td style="padding-right: 80px;" valign="top">
						<table id="syncChangesTable" cellpadding="4" cellspacing="0">
							<thead>
								<tr>
									<th colspan="2" valign="bottom"><spring:message code="sync.config.class.item" /></th>
									<th colspan="2" align="center">&nbsp;&nbsp;<spring:message code="sync.general.default.behavior" /></th>
								</tr>
							</thead>
							<tbody id="globalPropsList">
								<c:if test="${not empty syncClassGroupsLeft}">
									<c:forEach var="syncClasses" items="${syncClassGroupsLeft}" varStatus="status">
										<tr>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" colspan="2" align="left">
												<b>${syncClasses.key}</b>
											</td>
											<td style="padding-right: 20px; border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'to');" id="to_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="groupToDefault" value="true" <c:if test="${syncClassGroupTo[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="sync.config.class.defaultTo" /></b></span>
											</td>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'from');" id="from_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px; margin-right: 1px;" type="checkbox" name="groupFromDefault" value="true" <c:if test="${syncClassGroupFrom[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="sync.config.class.defaultFrom" /></b></span>
											</td>
										</tr>
										<c:if test="${not empty syncClasses.value}">
											<c:forEach var="syncClass" items="${syncClasses.value}" varStatus="statusClass">
												<tr>
													<td>&nbsp;</td>
													<td align="left">
														${syncClass.name}
													</td>
													<td align="center" style="padding-right: 20px;">
														<input id="to_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="toDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultTo}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="sync.config.class.defaultTo" /></span>
													</td>
													<td align="center">
														<input id="from_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="fromDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultFrom}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="sync.config.class.defaultFrom" /></span>
													</td>
												</tr>
											</c:forEach>
										</c:if>
										<c:if test="${empty syncClasses.value}">
											<td colspan="5" align="left">
												<i><spring:message code="sync.config.classes.none" /></i>
											</td>
										</c:if>
									</c:forEach>
								</c:if>
								<c:if test="${empty syncClassGroupsLeft}">
									<td colspan="4" align="left">
										<i><spring:message code="sync.config.classes.none" /></i>
									</td>
								</c:if>
							</tbody>
						</table>
					</td>
					<td valign="top">
						<table id="syncChangesTable" cellpadding="4" cellspacing="0">
							<thead>
								<tr>
									<th colspan="2" valign="bottom"><spring:message code="sync.config.class.item" /></th>
									<th colspan="2" align="center">&nbsp;&nbsp;<spring:message code="sync.general.default.behavior" /></th>
								</tr>
							</thead>
							<tbody id="globalPropsList">
								<c:if test="${not empty syncClassGroupsRight}">
									<c:forEach var="syncClasses" items="${syncClassGroupsRight}" varStatus="status">
										<tr>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" colspan="2" align="left">
												<b>${syncClasses.key}</b>
											</td>
											<td style="padding-right: 20px; border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'to');" id="to_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="groupToDefault" value="true" <c:if test="${syncClassGroupTo[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="sync.config.class.defaultTo" /></b></span>
											</td>
											<td style="border-top: 1px solid #aaa; background-color: whitesmoke;" align="center">
												<input onclick="toggleChecks('${syncClasses.key}', 'from');" id="from_${syncClasses.key}" style="margin-top: 0px; margin-bottom: 0px; margin-right: 1px;" type="checkbox" name="groupFromDefault" value="true" <c:if test="${syncClassGroupFrom[syncClasses.key]}">checked</c:if>
													 <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
												><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><b><spring:message code="sync.config.class.defaultFrom" /></b></span>
											</td>
										</tr>
										<c:if test="${not empty syncClasses.value}">
											<c:forEach var="syncClass" items="${syncClasses.value}" varStatus="statusClass">
												<tr>
													<td>&nbsp;</td>
													<td align="left">
														${syncClass.name}
													</td>
													<td align="center" style="padding-right: 20px;">
														<input id="to_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="toDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultTo}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="sync.config.class.defaultTo" /></span>
													</td>
													<td align="center">
														<input id="from_${syncClass.syncClassId}" style="margin-top: 0px; margin-bottom: 0px;" type="checkbox" name="fromDefault" value="${syncClass.syncClassId}" 
															<c:if test="${syncClass.defaultFrom}">checked</c:if> <c:if test="${syncClasses.key == 'REQUIRED'}">disabled</c:if>
														><span style="font-size: 0.9em;<c:if test="${syncClasses.key == 'REQUIRED'}"> color: #aaa;</c:if>"><spring:message code="sync.config.class.defaultFrom" /></span>
													</td>
												</tr>
											</c:forEach>
										</c:if>
										<c:if test="${empty syncClasses.value}">
											<td colspan="5" align="left">
												<i><spring:message code="sync.config.classes.none" /></i>
											</td>
										</c:if>
									</c:forEach>
								</c:if>
								<c:if test="${empty syncClassGroupsRight}">
									<td colspan="4" align="left">
										<i><spring:message code="sync.config.classes.none" /></i>
									</td>
								</c:if>
							</tbody>
						</table>
					</td>
				</tr>
				<tr>
					<td colspan="2" align="center">
						<input type="submit" value="<spring:message code="sync.general.save" />" />
						<input type="button" onclick="location.href='config.list';" value="<spring:message code="sync.general.cancel" />" />
					</td>
				</tr>
			</table>
		</div>

	</form>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
