package com.tallerwebi.dominio.recuperacion;

/** Manda mails de texto. La implementación usa SMTP (Gmail). */
@FunctionalInterface
public interface EnviadorDeMail {
  void enviar(String para, String asunto, String texto);
}
