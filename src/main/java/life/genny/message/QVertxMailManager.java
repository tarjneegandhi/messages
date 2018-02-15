package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.GoogleDocHelper;
import life.genny.util.MergeHelper;

public class QVertxMailManager implements QMessageProvider{
	
	private Vertx vertx;
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus) {
		
		vertx = Vertx.vertx();

		MailMessage mailmessage = mailMessage(vertx, message);
		MailClient mailClient = createClient(vertx);

		mailClient.sendMail(mailmessage, result -> {
			if (result.succeeded()) {
				System.out.println("email sent to ::"+mailmessage.getTo());
			} else {
				result.cause().printStackTrace();
			}
		});
	}	

	  public MailClient createClient(Vertx vertx) {
	    MailConfig config = new MailConfig();
	    config.setHostname(System.getenv("MAIL_SMTP_HOST"));
	    config.setPort(Integer.parseInt(System.getenv("MAIL_SMTP_PORT")));
	    config.setStarttls(StartTLSOptions.REQUIRED);
	    config.setUsername(System.getenv("EMAIL_USERNAME"));
	    config.setPassword(System.getenv("EMAIL_PASSWORD"));
	    MailClient mailClient = MailClient.createNonShared(vertx, config);
	    
	    return mailClient;
	  }

	  public MailMessage mailMessage(Vertx vertx, QBaseMSGMessage messageTemplate) {
	    MailMessage message = new MailMessage();
	    message.setFrom(messageTemplate.getSource());
	    message.setTo(messageTemplate.getTarget());
	    message.setSubject(messageTemplate.getSubject());
	    //message.setCc("Another User <another@example.net>");
	    message.setHtml(messageTemplate.getMsgMessageData());
	    
	    return message;
	  }

	  public void attachment(Vertx vertx, MailMessage message) {
	    MailAttachment attachment = new MailAttachment();
	    attachment.setContentType("text/plain");
	    attachment.setData(Buffer.buffer("attachment file"));

	    message.setAttachment(attachment);
	  }

	  public void inlineAttachment(Vertx vertx, MailMessage message) {
	    MailAttachment attachment = new MailAttachment();
	    attachment.setContentType("image/jpeg");
	    attachment.setData(Buffer.buffer("image data"));
	    attachment.setDisposition("inline");
	    attachment.setContentId("<image1@example.com>");

	    message.setInlineAttachment(attachment);
	  }

	@Override
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap,
			String recipient, String token) {
		return null;
	}

	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, BaseEntity> entityTemplateMap,
			String token) {
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		BaseEntity recipientBe = entityTemplateMap.get("RECIPIENT");
		
		if(recipientBe != null) {
			if (template != null) {
				String docId = template.getEmail_templateId();
				String htmlString = GoogleDocHelper.getGoogleDocString(docId);
				logger.info(ANSI_GREEN + "email doc ID from google sheet ::" + docId + ANSI_RESET);
				
				baseMessage = new QBaseMSGMessage();
				baseMessage.setSubject(template.getSubject());
				baseMessage.setMsgMessageData(MergeUtil.merge(htmlString, entityTemplateMap));
				baseMessage.setSource(System.getenv("EMAIL_USERNAME"));
				
				
				baseMessage.setTarget(MergeUtil.getBaseEntityAttrValueAsString(recipientBe, "PRI_EMAIL"));								
			} else {
				logger.error("NO TEMPLATE FOUND");
			}
		} else {
			logger.error("Recipient BaseEntity is NULL");
		}
		
		
		return baseMessage;
	}

}
