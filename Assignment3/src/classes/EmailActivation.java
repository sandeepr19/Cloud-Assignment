package classes;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailActivation {

	public static void sendEmail(String messageBody) {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("sandeepranganathan.123@gmail.com"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"sandumb@gmail.com"));
			msg.setSubject("dhadhadhadha ]=:::");
			msg.setText(messageBody);
			Transport.send(msg);
		} catch (AddressException e) {
			// ...
		} catch (MessagingException e) {
			// ...
		}
	}
}
