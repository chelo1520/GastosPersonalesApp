package com.tallerwebi.infraestructura.recuperacion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class EnviadorDeMailSmtpTest {

  @Test
  public void debeMandarElMailDesdeLaCuentaConfigurada() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    EnviadorDeMailSmtp enviador = new EnviadorDeMailSmtp(mailSender, "gastitos@gmail.com");

    enviador.enviar("lour@gastitos.com", "Asunto", "Texto");

    ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(mail.capture());
    assertEquals("Gastitos <gastitos@gmail.com>", mail.getValue().getFrom());
    assertArrayEquals(new String[] { "lour@gastitos.com" }, mail.getValue().getTo());
    assertEquals("Asunto", mail.getValue().getSubject());
    assertEquals("Texto", mail.getValue().getText());
  }

  @Test
  public void noDebeIntentarMandarNadaSinCuentaConfigurada() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    EnviadorDeMailSmtp enviador = new EnviadorDeMailSmtp(mailSender, "");

    enviador.enviar("lour@gastitos.com", "Asunto", "Texto");

    verifyNoInteractions(mailSender);
  }

  @Test
  public void noDebeCortarElPedidoCuandoFallaElServidorDeMail() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    doThrow(new MailSendException("sin conexión"))
      .when(mailSender)
      .send(any(SimpleMailMessage.class));
    EnviadorDeMailSmtp enviador = new EnviadorDeMailSmtp(mailSender, "gastitos@gmail.com");

    enviador.enviar("lour@gastitos.com", "Asunto", "Texto");

    verify(mailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  public void debeConfigurarGmailConStartTlsYSacarLosEspaciosDeLaContrasenaDeAplicacion() {
    EnviadorDeMailSmtp enviador = new EnviadorDeMailSmtp(
      "smtp.gmail.com",
      587,
      "gastitos@gmail.com",
      "abcd efgh ijkl mnop"
    );

    JavaMailSenderImpl sender = (JavaMailSenderImpl) enviador.getMailSender();
    assertEquals("smtp.gmail.com", sender.getHost());
    assertEquals(587, sender.getPort());
    assertEquals("abcdefghijklmnop", sender.getPassword());
    assertTrue(
      Boolean.parseBoolean(
        sender.getJavaMailProperties().getProperty("mail.smtp.starttls.required")
      )
    );
  }
}
