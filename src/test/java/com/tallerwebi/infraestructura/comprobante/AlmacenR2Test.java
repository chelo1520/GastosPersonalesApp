package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.comprobante.AlmacenNoDisponible;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class AlmacenR2Test {

  @Test
  public void sinConfiguracionNoEstaDisponibleYNoGuarda() {
    AlmacenR2 almacen = new AlmacenR2("", "clave", "secreto", "bucket");

    assertFalse(almacen.disponible());
    assertThrows(
      AlmacenNoDisponible.class,
      () -> almacen.guardar("comprobantes/1/a.png", new byte[] { 1 }, "image/png")
    );
    almacen.destroy();
  }

  @Test
  public void conTodaLaConfiguracionArmaElClienteSinConectarse() {
    assertNotNull(AlmacenR2.crearCliente("cuenta", "clave", "secreto", "bucket"));
    assertNull(AlmacenR2.crearCliente("cuenta", "clave", " ", "bucket"));
    assertNull(AlmacenR2.crearCliente("cuenta", null, "secreto", "bucket"));
    assertNull(AlmacenR2.crearCliente("cuenta", "clave", "secreto", ""));
  }

  @Test
  public void debeSubirElArchivoAlBucketConSuClaveYTipo() {
    S3Client cliente = mock(S3Client.class);
    AlmacenR2 almacen = new AlmacenR2(cliente, "gastitos-comprobantes");

    almacen.guardar("comprobantes/7/abc.pdf", new byte[] { 1, 2 }, "application/pdf");

    assertTrue(almacen.disponible());
    ArgumentCaptor<PutObjectRequest> pedido = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(cliente).putObject(pedido.capture(), any(RequestBody.class));
    assertEquals("gastitos-comprobantes", pedido.getValue().bucket());
    assertEquals("comprobantes/7/abc.pdf", pedido.getValue().key());
    assertEquals("application/pdf", pedido.getValue().contentType());
  }

  @Test
  public void siR2FallaDebeAvisarQueNoEstaDisponible() {
    S3Client cliente = mock(S3Client.class);
    when(cliente.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
      .thenThrow(SdkClientException.create("sin conexión"));
    AlmacenR2 almacen = new AlmacenR2(cliente, "bucket");

    assertThrows(
      AlmacenNoDisponible.class,
      () -> almacen.guardar("comprobantes/1/a.png", new byte[] { 1 }, "image/png")
    );
  }

  @Test
  public void alCerrarLaAppDebeCerrarElCliente() {
    S3Client cliente = mock(S3Client.class);

    new AlmacenR2(cliente, "bucket").destroy();

    verify(cliente).close();
  }
}
