package com.tallerwebi.infraestructura.comprobante;

import com.tallerwebi.dominio.comprobante.AlmacenComprobantes;
import com.tallerwebi.dominio.comprobante.AlmacenNoDisponible;
import java.net.URI;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Los comprobantes en un bucket privado de Cloudflare R2. R2 habla la API de S3, así que se usa el
 * cliente S3 de AWS apuntando al endpoint de la cuenta de Cloudflare. Las credenciales vienen de
 * variables de entorno (.env); si falta alguna, el almacén queda no disponible y la app sigue.
 */
@Component
public class AlmacenR2 implements AlmacenComprobantes, DisposableBean {

  // R2 no tiene regiones: el SDK pide una y "auto" es la que indica Cloudflare
  private static final Region REGION_R2 = Region.of("auto");

  private final S3Client cliente;
  private final String bucket;

  @Autowired
  public AlmacenR2(
    @Value("${R2_ACCOUNT_ID:}") String cuenta,
    @Value("${R2_ACCESS_KEY_ID:}") String claveDeAcceso,
    @Value("${R2_SECRET_ACCESS_KEY:}") String secreto,
    @Value("${R2_BUCKET:}") String bucket
  ) {
    this(crearCliente(cuenta, claveDeAcceso, secreto, bucket), bucket);
  }

  AlmacenR2(S3Client cliente, String bucket) {
    this.cliente = cliente;
    this.bucket = bucket;
  }

  // null si falta algún dato de la configuración
  static S3Client crearCliente(String cuenta, String claveDeAcceso, String secreto, String bucket) {
    if (vacio(cuenta) || vacio(claveDeAcceso) || vacio(secreto) || vacio(bucket)) {
      return null;
    }
    return S3Client
      .builder()
      .endpointOverride(URI.create("https://" + cuenta + ".r2.cloudflarestorage.com"))
      .region(REGION_R2)
      .credentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create(claveDeAcceso, secreto))
      )
      // Las versiones nuevas del SDK agregan sumas de control que R2 no siempre acepta: solo cuando
      // la operación las exige
      .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
      .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
      .build();
  }

  private static boolean vacio(String valor) {
    return valor == null || valor.isBlank();
  }

  @Override
  public boolean disponible() {
    return cliente != null;
  }

  @Override
  public void guardar(String clave, byte[] contenido, String tipo) {
    if (!disponible()) {
      throw new AlmacenNoDisponible("El almacén de comprobantes no está configurado");
    }
    try {
      cliente.putObject(
        PutObjectRequest.builder().bucket(bucket).key(clave).contentType(tipo).build(),
        RequestBody.fromBytes(contenido)
      );
    } catch (SdkException e) {
      throw new AlmacenNoDisponible("No se pudo guardar el comprobante en R2", e);
    }
  }

  @Override
  public void destroy() {
    if (cliente != null) {
      cliente.close();
    }
  }
}
