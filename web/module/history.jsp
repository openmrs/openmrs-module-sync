<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/history.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<h2><spring:message code="sync.history.title"/></h2>

<b class="boxHeader"><spring:message code="sync.changes.all"/></b>
<div class="box">
	<table id="syncChangesTable" cellpadding="7" cellspacing="0">
		<thead>
			<tr>
				<th><spring:message code="sync.status.itemTypeAndUuid" /></th>
				<%--
				<th colspan="2" style="text-align: center;"><spring:message code="sync.status.timestamp" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.itemState" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.recordState" /></th>
				<th nowrap style="text-align: center;"><spring:message code="sync.status.retryCount" /></th>
				--%>
				<c:if test="${not empty servers}">
					<c:forEach items="${servers}" var="server">
						<th style="text-align: center; <c:if test="${server.serverType == 'PARENT'}">background-color: #eef;</c:if>">
							${server.nickname}
							<c:if test="${server.serverType == 'PARENT'}">(<spring:message code="${server.serverType}" />)</c:if>
						</th>
					</c:forEach>
				</c:if>
				<c:if test="${empty servers}">
					<th style="font-weight: normal;"><i><spring:message code="sync.status.servers.none" /></i></th>
				</c:if>
			</tr>
		</thead>
		<tbody id="globalPropsList">
			<c:if test="${not empty syncRecords}">
				<c:set var="bgStyle" value="eee" />
				<c:set var="bgStyleParent" value="dde" />
				<c:forEach var="syncRecord" items="${syncRecords}" varStatus="status">
					<%--<c:forEach var="syncItem" items="${syncRecord.items}" varStatus="itemStatus">--%>
						<tr>
							<td valign="middle" nowrap style="background-color: #${bgStyle};">
								<b><a href="viewrecord.form?uuid=${syncRecord.uuid}">${recordTypes[syncRecord.uuid]}</a></b>
								<c:if test="${not empty recordText[syncRecord.uuid]}">
									(${recordText[syncRecord.uuid]})
								</c:if>
								<br>
								<span style="color: #bbb">
									<spring:message code="sync.item.state_${recordChangeType[syncRecord.uuid]}" /> -
									<openmrs:formatDate date="${syncRecord.timestamp}" format="${syncDateDisplayFormat}" />	
									<%--<c:if test="${not empty itemInfo[syncItem.key.keyValue]}">(${itemInfo[syncItem.key.keyValue]})</c:if></b>--%>
								</span>
							</td>
							<c:if test="${not empty servers}">
								<c:forEach items="${servers}" var="server">
									<td valign="middle" nowrap style="background-color: #<c:if test="${server.serverType == 'PARENT'}">${bgStyleParent}</c:if><c:if test="${server.serverType != 'PARENT'}">${bgStyle}</c:if>;" align="center">
										<c:choose>
											<c:when test="${server.serverType == 'PARENT'}">
												<span class="sync${syncRecord.state}"><spring:message code="sync.record.state_${syncRecord.state}" /></span>
											</c:when>
											<c:otherwise>
												<c:if test="${not empty syncRecord.remoteRecords[server]}">
													<span class="sync${syncRecord.remoteRecords[server].state}"><spring:message code="sync.record.state_${syncRecord.remoteRecords[server].state}" /></span>
												</c:if>
												<c:if test="${empty syncRecord.remoteRecords[server]}">
													<span style="color: #bbb"><i><spring:message code="sync.record.server.didNotExist" /></i></span>
												</c:if>
											</c:otherwise>
										</c:choose>
									</td>
								</c:forEach>
							</c:if>
							<c:if test="${empty servers}">
								<td></td>
							</c:if>
						</tr>
						<c:choose>
							<c:when test="${bgStyle == 'eee'}">
								<c:set var="bgStyle" value="fff" />
								<c:set var="bgStyleParent" value="eef" />
							</c:when>
							<c:otherwise>
								<c:set var="bgStyle" value="eee" />
								<c:set var="bgStyleParent" value="dde" />
							</c:otherwise>
						</c:choose>
					<%--</c:forEach>--%>
					
				</c:forEach>
			</c:if>
			<c:if test="${empty syncRecords}">
				<tr>
					<td colspan="5" align="left">
						<i><spring:message code="sync.history.noItems" /></i>
					</td>
				</tr>
			</c:if>
		</tbody>
	</table>
	
	<a href="?firstRecordId=${firstRecordId - size}&size=${size}" disabled="disabled">&larr; <spring:message code="sync.general.newer"/></a>
	<a href="?firstRecordId=${firstRecordId + size}&size=${size}"><spring:message code="sync.general.older"/> &rarr;</a>
	
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>