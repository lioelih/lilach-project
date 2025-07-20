package il.cshaifasweng.OCSFMediatorExample.server;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Authenticator;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.List;
import java.util.Properties;

public class EmailService {
    // ← configure these for your real SMTP server
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_USER     = "lilachstoresupp@gmail.com";
    private static final String SMTP_PASS     = "qvwa qcvn dynx lwom ";
    private static final String SUPPORT_EMAIL = "support@lilachstore.com";

    // single shared Session
    private static final Session session = Session.getInstance(
            new Properties() {{
                put("mail.smtp.auth",           "true");
                put("mail.smtp.starttls.enable","true");
                put("mail.smtp.host",           SMTP_HOST);
                put("mail.smtp.port",           "587");
                put("mail.smtp.ssl.trust",       SMTP_HOST);
            }},
            new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                }
            }
    );

    /**
     * Send a “your order has been received” email.
     *
     * @param toAddress     the customer’s email
     * @param username      their login name
     * @param orderId       which order
     * @param toSelf        true if recipient==the user
     * @param recipientName name on the order form
     * @param fulfilType    "PICKUP" or "DELIVERY"
     * @param fulfilInfo    branch name or full address
     * @param products      list of product names
     * @param totalPaid     how much they paid (₪)
     */
    public static void sendOrderReceivedEmail(
            String toAddress,
            String username,
            int orderId,
            boolean toSelf,
            String recipientName,
            String fulfilType,
            String fulfilInfo,
            List<String> products,
            double totalPaid
    ) throws MessagingException {
        String subject = String.format("Order Update – %d", orderId);
        String whom    = toSelf ? "you" : recipientName;

        StringBuilder body = new StringBuilder()
                .append(String.format("Hello %s,%n%n", username))
                .append(String.format("Order #%d has been successfully received by %s.%n%n", orderId, whom))
                .append("Further details:\n");

        if ("PICKUP".equals(fulfilType)) {
            body.append("- Pick Up Branch: ").append(fulfilInfo).append("\n");
        } else {
            body.append("- Delivered to: ").append(fulfilInfo).append("\n");
        }

        body.append("\n- Products:\n");
        for (String p : products) {
            body.append("   • ").append(p).append("\n");
        }

        body.append(String.format("%n- Total Paid: ₪%.2f%n%n", totalPaid))
                .append("For further questions contact support at ")
                .append(SUPPORT_EMAIL)
                .append("\n\nThank you!");

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_USER));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
        msg.setSubject(subject);
        msg.setText(body.toString());

        Transport.send(msg);
    }
}
