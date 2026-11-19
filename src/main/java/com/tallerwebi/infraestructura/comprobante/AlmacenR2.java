package com.tallerwebi.infraestructura.comprobante;

import com.tallerwebi.dominio.comprobante.AlmacenComprobantes;
import com.tallerwebi.dominio.comprobante.AlmacenNoDisponible;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Los comprobantes en un bucket privado de Cloudflare R2. R2 habla la API de S3, así que se usa el
 * cliente S3 de AWS apuntando al endpoint de la cuenta de Cloudflare. Las credenciales vienen de
 * variables de entorno (.env); si falta alguna, el almacén queda no disponible y la app sigue.
 */
@Component
public class AlmacenR2 implements AlmacenComprobantes, DisposableBean {

  // R2 no tiene regiones: el SDK pide una y "auto" es la que indica Cloudflare
  private static final Region REGION_R2 = Region.of("auto");
  // Lo que dura el link para ver un comprobante: alcanza para abrirlo, no para compartirlo
  static final Duration VIGENCIA_URL = Duration.ofMinutes(5);

  private final S3Client cliente;
  private final S3Presigner firmador;
  private final String bucket;

  @Autowired
  public AlmacenR2(
    @Value("${R2_ACCOUNT_ID:}") String cuenta,
    @Value("${R2_ACCESS_KEY_ID:}") String claveDeAcceso,
    @Value("${R2_SECRET_ACCESS_KEY:}") String secreto,
    @Value("${R2_BUCKET:}") String bucket
  ) {
    this(
      crearCliente(cuenta, claveDeAcceso, secreto, bucket),
      crearFirmador(cuenta, claveDeAcceso, secreto, bucket),
      bucket
    );
  }

  AlmacenR2(S3Client cliente, S3Presigner firmador, String bucket) {
    this.cliente = cliente;
    this.firmador = firmador;
    this.bucket = bucket;
  }

  // null si falta algún dato de la configuración
  static S3Client crearCliente(String cuenta, String claveDeAcceso, String secreto, String bucket) {
    if (!configurado(cuenta, claveDeAcceso, secreto, bucket)) {
      return null;
    }
    return S3Client
      .builder()
      .endpointOverride(endpoint(cuenta))
      .region(REGION_R2)
      .credentialsProvider(credenciales(claveDeAcceso, secreto))
      // Las versiones nuevas del SDK agregan sumas de control que R2 no siempre acepta: solo cuando
      // la operación las exige
      .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
      .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
      .build();
  }

  // Firma los links temporales sin conectarse a R2; null si falta algún dato de la configuración
  static S3Presigner crearFirmador(
    String cuenta,
    String claveDeAcceso,
    String secreto,
    String bucket
  ) {
    if (!configurado(cuenta, claveDeAcceso, secreto, bucket)) {
      return null;
    }
    return S3Presigner
      .builder()
      .endpointOverride(endpoint(cuenta))
      .region(REGION_R2)
      .credentialsProvider(credenciales(claveDeAcceso, secreto))
      .build();
  }

  private static boolean configurado(String... valores) {
    for (String valor : valores) {
      if (valor == null || valor.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private static URI endpoint(String cuenta) {
    return URI.create("https://" + cuenta + ".r2.cloudflarestorage.com");
  }

  private static AwsCredentialsProvider credenciales(String claveDeAcceso, String secreto) {
    return StaticCredentialsProvider.create(AwsBasicCredentials.create(claveDeAcceso, secreto));
  }

  @Override
  public boolean disponible() {
    return cliente != null && firmador != null;
  }

  private void exigirDisponible() {
    if (!disponible()) {
      throw new AlmacenNoDisponible("El almacén de comprobantes no está configurado");
    }
  }

  @Override
  public void guardar(String clave, byte[] contenido, String tipo) {
    exigirDisponible();
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
  public String urlTemporal(String clave) {
    exigirDisponible();
    GetObjectPresignRequest pedido = GetObjectPresignRequest
      .builder()
      .signatureDuration(VIGENCIA_URL)
      .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(clave).build())
      .build();
    return firmador.presignGetObject(pedido).url().toString();
  }

  @Override
  public void eliminar(String clave) {
    exigirDisponible();
    try {
      cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
    } catch (SdkException e) {
      throw new AlmacenNoDisponible("No se pudo borrar el comprobante de R2", e);
    }
  }

  @Override
  public void destroy() {
    if (cliente != null) {
      cliente.close();
    }
    if (firmador != null) {
      firmador.close();
    }
  }
}
