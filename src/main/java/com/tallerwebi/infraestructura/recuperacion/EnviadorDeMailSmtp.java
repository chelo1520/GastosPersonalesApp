package com.tallerwebi.infraestructura.recuperacion;

import com.tallerwebi.dominio.recuperacion.EnviadorDeMail;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Manda los mails por SMTP. Por defecto usa Gmail: MAIL_USERNAME es la cuenta y MAIL_PASSWORD una
 * "contraseña de aplicación" de Google (no la contraseña de la cuenta).
 */
@Component
public class EnviadorDeMailSmtp implements EnviadorDeMail {

  private static final Logger LOG = Logger.getLogger(EnviadorDeMailSmtp.class.getName());
  private static final String TIMEOUT_MS = "10000";

  private final JavaMailSender mailSender;
  private final String remitente;

  @Autowired
  public EnviadorDeMailSmtp(
    @Value("${MAIL_HOST:smtp.gmail.com}") String host,
    @Value("${MAIL_PORT:587}") int puerto,
    @Value("${MAIL_USERNAME:}") String usuario,
    @Value("${MAIL_PASSWORD:}") String password
  ) {
    this(crearMailSender(host, puerto, usuario, password), usuario);
  }

  EnviadorDeMailSmtp(JavaMailSender mailSender, String remitente) {
    this.mailSender = mailSender;
    this.remitente = remitente;
  }

  @Override
  public void enviar(String para, String asunto, String texto) {
    if (remitente == null || remitente.isBlank()) {
      LOG.warning("No se mandó el mail: falta configurar MAIL_USERNAME y MAIL_PASSWORD en el .env");
      return;
    }

    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom("Gastitos <" + remitente + ">");
    mail.setTo(para);
    mail.setSubject(asunto);
    mail.setText(texto);
    try {
      mailSender.send(mail);
    } catch (MailException e) {
      // Quien lo pidió ve el mismo mensaje que siempre; el error queda en el log del servidor
      LOG.log(Level.SEVERE, "No se pudo mandar el mail", e);
    }
  }

  // Para revisar la configuración en los tests
  JavaMailSender getMailSender() {
    return mailSender;
  }

  private static JavaMailSender crearMailSender(
    String host,
    int puerto,
    String usuario,
    String password
  ) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(puerto);
    sender.setUsername(usuario);
    // Google muestra la contraseña de aplicación con espacios ("abcd efgh ..."); se aceptan igual
    sender.setPassword(password == null ? null : password.replace(" ", ""));
    sender.setDefaultEncoding("UTF-8");

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");
    props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
    props.put("mail.smtp.timeout", TIMEOUT_MS);
    props.put("mail.smtp.writetimeout", TIMEOUT_MS);
    return sender;
  }
}
