package com.tallerwebi.dominio.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ArchivoComprobanteTest {

  @Test
  public void laHuellaEsElSha256DelContenido() {
    ArchivoComprobante archivo = ArchivoComprobante.de(
      "abc".getBytes(StandardCharsets.UTF_8),
      "image/png"
    );

    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      archivo.huella()
    );
  }

  @Test
  public void elMismoContenidoDaLaMismaHuellaYOtroContenidoOtra() {
    byte[] contenido = { 1, 2, 3 };

    assertEquals(
      ArchivoComprobante.de(contenido, "image/png").huella(),
      ArchivoComprobante.de(contenido.clone(), "image/jpeg").huella()
    );
    assertNotEquals(
      ArchivoComprobante.de(contenido, "image/png").huella(),
      ArchivoComprobante.de(new byte[] { 1, 2, 4 }, "image/png").huella()
    );
  }

  @Test
  public void laClaveVaEnLaCarpetaDelUsuarioConLaExtensionDelTipo() {
    byte[] contenido = { 1, 2, 3 };
    String huella = ArchivoComprobante.de(contenido, "image/png").huella();

    assertEquals(
      "comprobantes/7/" + huella + ".png",
      ArchivoComprobante.de(contenido, "image/png").clave(7L)
    );
    assertEquals(
      "comprobantes/7/" + huella + ".jpg",
      ArchivoComprobante.de(contenido, "image/jpeg").clave(7L)
    );
    assertEquals(
      "comprobantes/7/" + huella + ".pdf",
      ArchivoComprobante.de(contenido, LectorComprobante.TIPO_PDF).clave(7L)
    );
  }
}
