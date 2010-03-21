<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="View Synchronization Status"
	otherwise="/login.htm"
	redirect="/module/sync/viewrecord.form" />

<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="localHeader.jsp"%>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/engine.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />

<script language="JavaScript">
	var currentKey="";
	var currentUuid="";
	var rootElt="";

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
		function changeLinksTab(tabId) {
			var	tabObj = document.getElementById(tabId);
			var tabs = tabObj.parentNode.getElementsByTagName('a');
			for(var i=0;i<tabs.length;++i){
				if(tabs[i].id==tabId){
					tabs[i].style.fontWeight='bold';
				}
				else{
					tabs[i].style.fontWeight='normal';
				}
			}
	}
		function showContentResult(result) {
			DWRUtil.setValue("loadContent", "");
			domParser = new DOMParser();
			xmlDocument = domParser.parseFromString(result,'application/xml');
			root=xmlDocument.documentElement;
			rootElt=root.nodeName;
			cn=root.childNodes;
			newHTML="<form id='contentForm' action='' method=''><table width='100%' border='0' cellspacing='0' cellpadding='0'>";
			for(i=0;i<cn.length;i++){
				newHTML+="<tr class='syncTr' ";
				if(i%2==0)newHTML+="bgcolor='#C4D9D9'";
				else newHTML+="bgcolor='#E7EFEF'";
				newHTML+="><td><strong>"+cn[i].nodeName+"</strong></td><td>"+cn[i].attributes[0].value+"</td><td height='30'><input id='field'"+i+" type='text' value='";
				if(cn[i].childNodes.length==1)
					newHTML+=cn[i].childNodes[0].nodeValue;
				newHTML+="' size='50' /></td></tr>";
			}
			newHTML+="<tr><td>&nbsp;</td><td>&nbsp;</td><td height='35' align='left' valign='bottom'><input type='button' name='saveButton' value='  Save  ' onclick='setSyncItemContent();'/></td></tr>";
			newHTML+="</table></form>";
			document.getElementById("contents").innerHTML = newHTML;
			
		}
	
		function getSyncItemContent() {
			DWRUtil.setValue("loadContent","Loading Item payload ...");
			DWRSyncService.getSyncItemContent(currentUuid, currentKey,showContentResult);
			
		}
		function showResult(result) {
			DWRUtil.setValue("loadContent", result);
			setTimeout("DWRUtil.setValue('loadContent','');",3000);
			DWRUtil.setValue("loadContent", "");
			document.getElementById("contents").innerHTML = "";
			currentUuid="";
			currentKey="";
			
		}
		function setSyncItemContent() {
			if(currentKey=="")return;
			if(currentUuid=="")return;
			
			cnt="";
			tr=document.getElementById('contentForm').childNodes[0].childNodes[0].childNodes;
			cnt+="<"+rootElt+">";
			for(i=0;i<(tr.length-1);i++){
				td=tr[i].childNodes;
				cnt+="<"+td[0].childNodes[0].childNodes[0].nodeValue+"";
				cnt+=' type="'+td[1].childNodes[0].nodeValue+'">';
				cnt+=td[2].childNodes[0].value;
				cnt+="</"+td[0].childNodes[0].childNodes[0].nodeValue+">";
			}
			cnt+="</"+rootElt+">";
			DWRUtil.setValue("loadContent","Saving Item payload ...");
			DWRSyncService.setSyncItemContent(currentUuid, currentKey,cnt,showResult);
			
		}
</script>
<h2><spring:message code="sync.record.details.title" /></h2>
<b class="boxHeader"><spring:message
	code="sync.record.details.view_edit" /></b>
<c:if test="${record!=null}">
	<div class="box"><span><b><spring:message
		code="sync.record.details.record" /> ${record.recordId} </b></span><span>
	<h3>${mainClassName}: ${displayName}</h3>
	<h5><spring:message code="sync.item.state_${mainState}" />
	<span><b><spring:message
		code="sync.record.details.on" /></b> <openmrs:formatDate
		date="${record.timestamp}" format="${syncDateDisplayFormat}" /> </span><br />
	</h5>
	<h5><span><b><spring:message
		code="sync.record.details.status" /></b></span><span
		class="sync${record.state}"><spring:message
		code="sync.record.state_${record.state}" /></span><br />
	<span><b><spring:message
		code="sync.record.details.retry_count" /></b></span><span>${record.retryCount}</span></br>
	<br>
	<span>
	<ul>
		<c:if
			test="${record.state!='NEW' && record.state!='COMMITTED' && record.state!='ALREADY_COMMITTED'}">
			<li><b><a
				href="viewrecord.form?uuid=${record.uuid}&action=reset"><spring:message
				code="sync.record.details.reset" /></a></b></li>
		</c:if>
		<br/>
		<c:if
			test="${record.state!='NOT_SUPPOSED_TO_SYNC' && record.state!='COMMITTED' && record.state!='ALREADY_COMMITTED'}">
			<li><b><a
				href="viewrecord.form?uuid=${record.uuid}&action=remove"><spring:message
				code="sync.record.details.remove" /></a></b></li>
		</c:if>
	</ul>
	</span><br />
	</h5>
	</span>

<div class="innerBoxHeader"><span style="font-weight: bold"><spring:message code="sync.record.details.payload" /></div>
	<div class="innerBox">
	<br/>
	<span style="font-weight: bold">${itemsNumber} <spring:message code="sync.record.details.classes" /> </span> <c:forEach var="syncItem" items="${syncItems}"
		varStatus="status">
		<a id="item_${syncItem.key.keyValue}" href="#"
			onclick="javascript:changeLinksTab(this.id);currentUuid='${record.uuid}';currentKey='${syncItem.key.keyValue}';javascript:getSyncItemContent();">${itemTypes[syncItem.key.keyValue]}
		(${syncItem.state})</a> | 
		</c:forEach>
		<br/><br/>
		
	<div align="center">
		<span id="loadContent" align="center" style="font: Batang; font-weight: bold; color: #0066FF"></span>
	</div>
	
	<div id="contents" align="center">
	</div>
	
	</div>
	<table width="99%" border="0" cellpadding="0" cellspacing="0">
		<tr>
			<td width="25%" align="right" valign="middle"><c:if
				test="${hasPrevious==true}">
				<b><a href="?uuid=${record.uuid}&action=previous"><spring:message
					code="sync.record.details.prev" /></a></b>
			</c:if></td>
			<td width="25%" align="center" valign="middle"><b>${record.recordId}</b></td>
			<td width="25%" align="left" valign="middle"><c:if
				test="${hasNext==true}">
				<b><a href="?uuid=${record.uuid}&action=next"><spring:message
					code="sync.record.details.next" /></a></b>
			</c:if></td>
			<td width="25%" align="right" valign="middle"></td>
		</tr>
	</table>
	</div>
</c:if>
<c:if test="${record==null}">
	<span style="font-weight: bold"><spring:message
		code="sync.record.details.no.record" /></span>
</c:if>
