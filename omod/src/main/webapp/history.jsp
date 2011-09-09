<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/history.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/moduleResources/sync/sync.css" />

<script type="text/javascript">

	function reloadPage(firstRecordId) {
    	var dropdown = document.getElementById("itemsPerPage");
   		var index = dropdown.selectedIndex;
    	var ddVal = dropdown.options[index].value;
		document.location = "?firstRecordId=" + firstRecordId + "&size=" + ddVal;
	}

	function getQueryParameter() {
		  var queryString = window.top.location.search.substring(1);
		  var parameterName = "size=";
		  if ( queryString.length > 0 ) {
		    begin = queryString.indexOf ( parameterName );
		    if ( begin != -1 ) {
		      begin += parameterName.length;
		      end = queryString.indexOf ( "&" , begin );
		        if ( end == -1 ) {
		        end = queryString.length
		      }
		      var size = unescape ( queryString.substring ( begin, end ) );
				var dropdown = document.getElementById("itemsPerPage");
				document.getElementById('itemsPerPage').value = size;
		    }
		  }
	}
 
	function getNewerItemsList(firstRecordId,flag) {
		var firstRecordNum = parseInt(firstRecordId);
		var dropdown = document.getElementById("itemsPerPage");
    	var index = dropdown.selectedIndex;
    	var ddVal = dropdown.options[index].value;
    	var ddNum = parseInt(ddVal);
    	parseInt(ddNum);
    	
    	if(flag == false){
    		firstRecordNum = firstRecordNum + ddNum;
    	}if(flag == true){
    		firstRecordNum -= ddNum;
   		}
   		document.location = "?firstRecordId=" + firstRecordNum + "&size=" + ddNum;   
	}
</script>

<h2><spring:message code="sync.history.title"/></h2>

<b class="boxHeader">
<table>
<tr>
<td><spring:message code="sync.changes.all"/>
</td>
<td>&nbsp;&nbsp;&nbsp;&nbsp;<spring:message code="sync.history.recordsPerPage"/>
<select id="itemsPerPage" name="itemsPerPage" onchange="reloadPage(${firstRecordId})">
	<option value="10">10</option>
	<option value="50">50</option>
	<option value="100">100</option>
	</select>
</td>
</tr>
</table>

</b>
<div class="box">
	<c:if test="${syncRecords[0].recordId != latestRecordId}">
	<a href="javascript: getNewerItemsList(${firstRecordId}, false)">&larr; <spring:message code="sync.general.newer"/></a>
	</c:if>
	<c:if test="${isEarliestRecord != true}">
	<a href="javascript: getNewerItemsList(${firstRecordId}, true)"><spring:message code="sync.general.older"/> &rarr;</a>
	</c:if>
	&#124;
	<a href="historyNextError.list?recordId=${firstRecordId}&size=${size}"><spring:message code="sync.general.nextError"/> &rarr;</a>
	
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
												<span class="sync${syncRecord.state}"><spring:message code="sync.record.state_${syncRecord.state}" /> </span>
												<br>
												<c:choose>
													<c:when test="${syncRecord.outgoing}">
														<span style="color: #bbb"><spring:message code="sync.record.direction.outgoing"/></span>
													</c:when>
													<c:otherwise>
														<spring:message code="sync.record.direction.incoming"/>
													</c:otherwise>
												</c:choose>
											</c:when>
											<c:otherwise>
												<c:if test="${not empty syncRecord.remoteRecords[server]}">
													<span class="sync${syncRecord.remoteRecords[server].state}"><spring:message code="sync.record.state_${syncRecord.remoteRecords[server].state}" /> </span>
													<br>
													<c:choose>
														<c:when test="${syncRecord.remoteRecords[server].outgoing}">
															<span style="color: #bbb"><spring:message code="sync.record.direction.outgoing"/></span>
														</c:when>
														<c:otherwise>
															<spring:message code="sync.record.direction.incoming"/>
														</c:otherwise>
													</c:choose>
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
					<td colspan="2" align="left">
						<i><spring:message code="sync.history.noItems" /></i>
					</td>
				</tr>
			</c:if>
		</tbody>
	</table>
	
	<c:if test="${syncRecords[0].recordId != latestRecordId}">
	<a href="javascript: getNewerItemsList(${firstRecordId}, false)">&larr; <spring:message code="sync.general.newer"/></a>
	</c:if>
	<c:if test="${isEarliestRecord != true}">
	<a href="javascript: getNewerItemsList(${firstRecordId}, true)"><spring:message code="sync.general.older"/> &rarr;</a>
	</c:if>
	&#124;
	<a href="historyNextError.list?recordId=${firstRecordId}&size=${size}"><spring:message code="sync.general.nextError"/> &rarr;</a>
	
</div>

<script>
window.onload=getQueryParameter(); 
</script>

<%@ include file="/WEB-INF/template/footer.jsp" %>