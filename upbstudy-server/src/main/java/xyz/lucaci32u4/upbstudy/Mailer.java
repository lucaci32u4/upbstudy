package xyz.lucaci32u4.upbstudy;

import lombok.extern.slf4j.Slf4j;

import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;

@Slf4j
public class Mailer {

    private final Config config;

    public Mailer(Config config) {
        this.config = config;
    }

    void sendMail(String dayhourContent) throws MailerException {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.smtpHost());
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", Integer.toString(config.smtpPort()));
        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        String username = config.smtpEmail().substring(0, config.smtpEmail().indexOf('@'));
                        return new PasswordAuthentication(username, config.smtpPassword());
                    }
                });
        try {
            MimeMessage message = new MimeMessage(session);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(config.emailDestination()));
            message.setSubject(config.emailSubject());

            Multipart multipart = new MimeMultipart();
            BodyPart textBodyPart = new MimeBodyPart();
            BodyPart formBodyPart = new MimeBodyPart();
            textBodyPart.setText(config.emailBody());
            multipart.addBodyPart(textBodyPart);
            multipart.addBodyPart(formBodyPart);

            formBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(config.assembleReservationFile(dayhourContent), "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
            formBodyPart.setFileName(config.attachmentName());

            message.setContent(multipart);

            Transport.send(message);
            log.info("Sent reservation for {}", dayhourContent);
        } catch (MessagingException e) {
            log.error("Failed to send reservation for {}", dayhourContent, e);
            throw new MailerException(e);
        }
    }

    public static class MailerException extends Exception {
        public MailerException(Throwable cause) {
            super(cause);
        }
    }

}
