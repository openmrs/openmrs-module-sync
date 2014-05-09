package org.openmrs.module.sync;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.notification.MessageException;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;

public class SyncMailUtil {

    private static final Log log = LogFactory.getLog(SyncMailUtil.class);
    private static Session mailSession = null;

    public static Session getMailSession() {
        if (mailSession == null ){
            AdministrationService adminService = Context.getAdministrationService();

            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", adminService.getGlobalProperty("mail.transport_protocol"));
            props.setProperty("mail.smtp.host", adminService.getGlobalProperty("mail.smtp_host"));
            props.setProperty("mail.smtp.port", adminService.getGlobalProperty("mail.smtp_port"));
            props.setProperty("mail.from", adminService.getGlobalProperty("mail.from"));
            props.setProperty("mail.debug", adminService.getGlobalProperty("mail.debug"));
            props.setProperty("mail.smtp.auth", adminService.getGlobalProperty("mail.smtp_auth"));
            props.setProperty("mail.smtp.starttls.enable", adminService.getGlobalProperty("mail.smtp.starttls.enable"));

            Authenticator auth = new Authenticator() {

                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(Context.getAdministrationService().getGlobalProperty("mail.user"),
                            Context.getAdministrationService().getGlobalProperty("mail.password"));
                }
            };

            mailSession = Session.getInstance(props, auth);
        }

        return mailSession;
    }

    public static void sendMessage(String recipients, String subject, String body)
            throws MessageException{
        try {
            Message message = new MimeMessage(getMailSession());
            message.setSentDate(new Date());
            if (StringUtils.isNotBlank(subject)) {
                message.setSubject(subject);
            }
            if (StringUtils.isNotBlank(recipients)) {
                for (String recipient : recipients.split("\\,")) {
                    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(recipient));
                }
            }
            if (StringUtils.isNotBlank(body)) {
                Multipart multipart = new MimeMultipart();
                MimeBodyPart contentBodyPart = new MimeBodyPart();
                contentBodyPart.setContent(body, "text/html");
                multipart.addBodyPart(contentBodyPart);
                message.setContent(multipart);
            }

            Transport.send(message);

        } catch (Exception e) {
            log.error("Message could not be sent due to " + e.getMessage(), e);
            throw new MessageException(e);
        }
    }
}
