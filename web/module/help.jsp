<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Synchronization Status" otherwise="/login.htm" redirect="/module/sync/help.htm" />

<%@ include file="/WEB-INF/template/header.jsp" %>

<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRSyncService.js" />

<h2><spring:message code="sync.help.title"/></h2>

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
			
	-->
</script>

<b class="boxHeader"><spring:message code="sync.help.heading"/></b>
<div class="box">

<b><spring:message code="sync.help.whatIsSynchronization" /></b>
<p><spring:message code="sync.help.whatIsSynchronizationAnswer" />
<p>
<b><spring:message code="sync.help.whatIsDifferenceSyncAndImportExport" /></b>
<p><spring:message code="sync.help.whatIsDifferenceSyncAndImportExportAnswer" />
<p>
<b><spring:message code="sync.help.howDoIUseSynchronization" /></b>
<p><spring:message code="sync.help.howDoIUseSynchronizationAnswer" />
<p>
<b><spring:message code="sync.help.howDoIConfigureParent" /></b>
<p><spring:message code="sync.help.howDoIConfigureParentAnswer" />
<p>
<b><spring:message code="sync.help.howDoISendToParentViaWeb" /></b>
<p><spring:message code="sync.help.howDoISendToParentViaWebAnswer" />
<p>
<b><spring:message code="sync.help.howDoISendToParentViaDisk" /></b>
<p><spring:message code="sync.help.howDoISendToParentViaDiskAnswer" />
<p>
<b><spring:message code="sync.help.whatDoTheErrorsMean" /></b>
<p><spring:message code="sync.help.whatDoTheErrorsMeanAnswer" />
<p>


</div>

<%@ include file="/WEB-INF/template/footer.jsp" %>
