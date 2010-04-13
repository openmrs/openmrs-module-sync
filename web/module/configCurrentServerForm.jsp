<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/configCurrentServer.form" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<script language="JavaScript">
	<!--
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

		function addNewClass(idAndName) {
			var userInputNode = document.getElementById(idAndName);
			var userInput = userInputNode.value;
			var newDiv = document.createElement("div");
			
			var newHiddenInput = document.createElement("input");
			newHiddenInput["type"] = "hidden";
			newHiddenInput["value"] = userInput;
			newHiddenInput["name"] = idAndName;
			newDiv.appendChild(newHiddenInput);
			
			newDiv.appendChild(document.createTextNode(userInput));
			
			var newDeleteImg = document.createElement("img");
			newDeleteImg.src = "${pageContext.request.contextPath}/images/delete.gif";
			newDeleteImg.onclick = function() {this.parentNode.parentNode.removeChild(this.parentNode);};
			newDiv.appendChild(newDeleteImg);

			document.getElementById(idAndName + "Div").appendChild(newDiv);
		}
			
	-->
</script>

<h2><spring:message code="sync.config.current"/></h2>

<div id="general">
	<form method="post" action="configCurrentServer.form">
		<input type="hidden" name="action" value="save" />
			
		<b class="boxHeader"><spring:message code="sync.config.server.configure"/></b>
		<div class="box">
			
			<table>
				<tr>
					<td align="right" valign="top">
						<b><spring:message code="sync.config.server.nickname" /></b>
					</td>
					<td align="left" valign="top">
						<input type="text" size="25" maxlength="250" id="serverName" name="serverName" value="${localServerName}" />
					</td>
					<td>
						<spring:message code="sync.config.advanced.serverName.info" />
					</td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverUuid" /></b></td>
					<td><input type="text" size="50" name="serverUuid" id="serverUuid" value="${localServerUuid}" /></td>
					<td><spring:message code="sync.config.advanced.serverUuid.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.serverAdminEmail" /></b></td>
					<td><input type="text" size="50" name="serverAdminEmail" id="serverAdminEmail" value="${localServerAdminEmail}" /></td>
					<td><spring:message code="sync.config.advanced.serverAdminEmail.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxPageRecords" /></b></td>
					<td><input type="text" size="6" name="maxPageRecords" id="maxPageRecords" value="${maxPageRecords}" /></td>
					<td><spring:message code="sync.config.advanced.maxPageRecords.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxRecords" /></b></td>
					<td><input type="text" size="6" name="maxRecords" id="maxRecords" value="${maxRecords}" /></td>
					<td><spring:message code="sync.config.advanced.maxRecords.info" /></td>
				</tr>
				<tr>
					<td align="right" nowrap><b><spring:message code="sync.config.advanced.maxRetryCount" /></b></td>
					<td><input type="text" size="6" name="maxRetryCount" id="maxRetryCount" value="${maxRetryCount}" /></td>
					<td><spring:message code="sync.config.advanced.maxRetryCount.info" /></td>
				</tr>
				<tr>
					<td></td>
					<td>
						<input type="submit" value="<spring:message code="general.save" />" />
						<input type="button" onClick="history.back();" value="<spring:message code="general.cancel" />" />					
					</td>
				</tr>
			</table>
		</div>		
	</form>
	
	<br>
	&nbsp;&nbsp;<a href="javascript://" onclick="showHideDiv('details');"><spring:message code="sync.general.showHideMoreOptions" /></a>
	<br>
	<br>
	
	<div id="details" style="display:none;">

		<b class="boxHeader"><spring:message code="sync.config.advanced.objects"/></b>
		<div class="box">
			<form method="post" action="configCurrentServer.form">
				<input type="hidden" name="action" value="saveClasses" />
				
				<i><spring:message code="sync.config.current.classes.description"/></i>
				
				<br/>
				
				<table id="syncChangesTable" cellpadding="4" cellspacing="0">
					<thead>
						<tr>
							<th align="center"><spring:message code="sync.config.current.defaultNotSend" /></th>
							<th align="center" style="border-left: 1px solid black"><spring:message code="sync.config.current.defaultNotReceive" /></th>
						</tr>
					</thead>
					<tbody>
						<c:if test="${fn:length(syncClasses) == 0}">
							<tr id="noClasses">
								<td colspan="2"><i><spring:message code="sync.config.classes.none" /></i></td>
							</tr>
						</c:if>
						<tr>
							<td valign="top">
								<div id="defaultNotSendToDiv">
									<c:forEach var="syncClass" items="${syncClasses}">
										<c:if test="${!syncClass.defaultSendTo}">
											<div>
												<input type="hidden" name="defaultNotSendTo" value="${syncClass.name}"/>
												${syncClass.name}
												<img src="${pageContext.request.contextPath}/images/delete.gif"
												     onclick="this.parentNode.parentNode.removeChild(this.parentNode)"/> 
												
											</div>
										</c:if>
									</c:forEach>
								</div>
								
								<c:forEach var="openmrsObjectClass" items="${openmrsObjectClasses}">
									${openmrsObjectClass.name}
								</c:forEach>
								
								<br/>
								<spring:message code="sync.config.addNewClass"/>:
								<br/>
								<input type="text" id="defaultNotSendTo" />
								<input type="button" id="newNotSendToButton" onClick="addNewClass('defaultNotSendTo');" value="<spring:message code="general.add"/>" />
								
							</td>
							<td valign="top" style="border-left: 1px solid black">
								<div id="defaultNotReceiveFromDiv">
									<c:forEach var="syncClass" items="${syncClasses}">
										<c:if test="${!syncClass.defaultReceiveFrom}">
											<div>
												<input type="hidden" name="defaultNotReceiveFrom" value="${syncClass.name}"/>
												${syncClass.name}
												<img src="${pageContext.request.contextPath}/images/delete.gif"
												     onclick="this.parentNode.parentNode.removeChild(this.parentNode)"/>
											</div> 
										</c:if>
									</c:forEach>
								</div>
								
								<br/>
								<spring:message code="sync.config.addNewClass"/>:
								<br/>
								<input type="text" id="defaultNotReceiveFrom" />
								<input type="button" id="defaultNotReceiveFromButton" onClick="addNewClass('defaultNotReceiveFrom');" value="<spring:message code="general.add"/>" />
								
							</td>
						</tr>
					</tbody>
				</table>
				
				<br/>
				
				<input type="submit" value="<spring:message code="general.save" />" />
				<input type="button" onclick="location.href='config.list';" value="<spring:message code="general.cancel" />" />
			</form>
		</div>
	</div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
