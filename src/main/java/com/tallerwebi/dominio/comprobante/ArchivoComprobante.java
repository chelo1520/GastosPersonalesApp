package com.tallerwebi.dominio.comprobante;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * El archivo de un comprobante tal como se subió, con su huella: el SHA-256 del contenido. El mismo
 * archivo da siempre la misma huella, así se reconoce si ya se cargó. Queda en la sesión entre la
 * lectura y la confirmación, por eso es Serializable.
 */
public record ArchivoComprobante(byte[] contenido, String tipo, String huella)
  implements Serializable {
  private static final String ALGORITMO_HUELLA = "SHA-256";

  public static ArchivoComprobante de(byte[] contenido, String tipo) {
    return new ArchivoComprobante(contenido, tipo, huellaDe(contenido));
  }

  static String huellaDe(byte[] contenido) {
    try {
      return HexFormat
        .of()
        .formatHex(MessageDigest.getInstance(ALGORITMO_HUELLA).digest(contenido));
    } catch (NoSuchAlgorithmException e) {
      // Toda JVM trae SHA-256: no pasa
      throw new IllegalStateException(e);
    }
  }

  // Dónde se guarda en el almacén: una carpeta por usuario y el nombre es la huella, así subir dos
  // veces el mismo archivo pisa el anterior en lugar de duplicarlo
  public String clave(Long idUsuario) {
    return "comprobantes/" + idUsuario + "/" + huella + "." + extension();
  }

  private String extension() {
    return switch (tipo) {
      case "image/png" -> "png";
      case LectorComprobante.TIPO_PDF -> "pdf";
      default -> "jpg";
    };
  }
}
