package org.openmrs.sync.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.server.RemoteServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class RegisterChildServerRestController {

    protected Log log = LogFactory.getLog(getClass());

    private static String MANAGE_SYNC_PRIVILEGE = "Manage Synchronization";

    @RequestMapping(value = "/rest/v1/sync/registerChildServer", method = RequestMethod.POST)
    @ResponseBody
    public Object registerChildServer(@RequestBody String childServer) {
        if (Context.hasPrivilege(MANAGE_SYNC_PRIVILEGE)) {
            RemoteServer server = null;
            ObjectMapper objectMapper = new ObjectMapper();
            JsonFactory jsonFactory = objectMapper.getJsonFactory();
            try {
                JsonParser jsonParser = jsonFactory.createJsonParser(childServer);
                JsonNode jsonNode = objectMapper.readTree(jsonParser);
                JsonNode childServerNode = jsonNode.get("childServer");
                if (childServerNode != null) {
                    String uuid = childServerNode.get("uuid").asText();
                    String nickname = childServerNode.get("nickname").asText();
                    String username = childServerNode.get("username").asText();
                    String password = childServerNode.get("password").asText();
                    List<String> notSendTo= new ArrayList<>();
                    List<String> notReceiveFrom = new ArrayList<>();
                    JsonNode notSendToNode = jsonNode.get("childServer").get("notSendTo");
                    if (notSendToNode != null) {
                        Iterator<JsonNode> elements = notSendToNode.getElements();
                        while (elements.hasNext()) {
                            notSendTo.add(elements.next().getTextValue());
                        }
                    }
                    JsonNode notReceiveFromNode = jsonNode.get("childServer").get("notReceiveFrom");
                    if (notReceiveFromNode != null) {
                        Iterator<JsonNode> elements = notReceiveFromNode.getElements();
                        while (elements.hasNext()) {
                            notReceiveFrom.add(elements.next().getTextValue());
                        }
                    }
                    server = Context.getService(SyncService.class).registerChildServer(nickname, uuid, username, password, notSendTo, notReceiveFrom);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body(server);
            } catch (Exception e) {
                log.error(e);
                return errorResponse(e);
            }

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        }
    }

    protected ResponseEntity<Map<String, Object>> errorResponse(Throwable t) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<String> errorMessages = new ArrayList<>();
        while (t != null && !errorMessages.contains(t.getMessage())) {
            errorMessages.add(t.getMessage());
            t = t.getCause();
        }
        data.put("errorMessages",errorMessages);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
    }
}
