package com.tallerwebi.dominio.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioComprobanteImplTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 19, 10, 30);
  private static final byte[] CONTENIDO = { (byte) 0x89, 0x50, 0x4e, 0x47 };

  private RepositorioComprobante repositorio;
  private AlmacenComprobantes almacen;
  private GastoServicio gastoServicio;
  private ServicioComprobante servicio;
  private Usuario usuario;
  private ArchivoComprobante archivo;

  @BeforeEach
  public void init() {
    repositorio = mock(RepositorioComprobante.class);
    almacen = mock(AlmacenComprobantes.class);
    gastoServicio = mock(GastoServicio.class);
    ZoneId zona = ZoneId.of("America/Argentina/Buenos_Aires");
    Clock reloj = Clock.fixed(AHORA.atZone(zona).toInstant(), zona);
    servicio = new ServicioComprobanteImpl(repositorio, almacen, gastoServicio, reloj);
    usuario = mock(Usuario.class);
    when(usuario.getId()).thenReturn(7L);
    archivo = ArchivoComprobante.de(CONTENIDO, "image/png");
  }

  @Test
  public void unComprobanteNuncaCargadoNoEstaRepetido() {
    servicio.verificarQueNoEsteCargado(archivo, usuario);

    verify(repositorio).buscar(usuario, archivo.huella());
  }

  @Test
  public void unComprobanteConGastosGuardadosEstaRepetido() {
    Comprobante anterior = new Comprobante(usuario, archivo.huella(), "image/png", AHORA);
    when(repositorio.buscar(usuario, archivo.huella())).thenReturn(anterior);
    when(repositorio.contarGastos(anterior)).thenReturn(2L);

    ComprobanteYaCargado error = assertThrows(
      ComprobanteYaCargado.class,
      () -> servicio.verificarQueNoEsteCargado(archivo, usuario)
    );

    assertEquals(
      "Este comprobante ya lo cargaste el 19/11/2026 (2 gastos). Si es otro ticket, sacale una " +
      "foto nueva.",
      error.getMessage()
    );
  }

  @Test
  public void unComprobanteCuyosGastosSeBorraronSePuedeVolverACargar() {
    Comprobante anterior = new Comprobante(usuario, archivo.huella(), "image/png", AHORA);
    when(repositorio.buscar(usuario, archivo.huella())).thenReturn(anterior);
    when(repositorio.contarGastos(anterior)).thenReturn(0L);

    servicio.verificarQueNoEsteCargado(archivo, usuario);
  }

  @Test
  public void debeGuardarLosGastosLigadosAlComprobanteYSubirElArchivo() {
    when(almacen.disponible()).thenReturn(true);
    List<Gasto> gastos = List.of(gasto(6300.0), gasto(3500.0));

    servicio.registrarGastos(gastos, archivo, usuario);

    ArgumentCaptor<Comprobante> guardado = ArgumentCaptor.forClass(Comprobante.class);
    verify(repositorio).guardar(guardado.capture());
    Comprobante comprobante = guardado.getValue();
    assertEquals(archivo.huella(), comprobante.getHuella());
    assertEquals("image/png", comprobante.getTipo());
    assertEquals(AHORA, comprobante.getFechaCarga());
    assertSame(comprobante, gastos.get(0).getComprobante());
    assertSame(comprobante, gastos.get(1).getComprobante());
    verify(gastoServicio).registrarGastos(gastos, usuario);
    String clave = "comprobantes/7/" + archivo.huella() + ".png";
    verify(almacen).guardar(clave, CONTENIDO, "image/png");
    assertEquals(clave, comprobante.getClave());
  }

  @Test
  public void sinAlmacenDebeGuardarLosGastosIgualSinArchivo() {
    when(almacen.disponible()).thenReturn(false);
    List<Gasto> gastos = List.of(gasto(6300.0));

    servicio.registrarGastos(gastos, archivo, usuario);

    verify(gastoServicio).registrarGastos(gastos, usuario);
    verify(almacen, never()).guardar(any(), any(), any());
    assertNull(gastos.get(0).getComprobante().getClave());
  }

  @Test
  public void siElAlmacenFallaDebeGuardarLosGastosIgualSinClave() {
    when(almacen.disponible()).thenReturn(true);
    doThrow(new AlmacenNoDisponible("caído")).when(almacen).guardar(any(), any(), any());
    List<Gasto> gastos = List.of(gasto(6300.0));

    servicio.registrarGastos(gastos, archivo, usuario);

    verify(gastoServicio).registrarGastos(gastos, usuario);
    assertNull(gastos.get(0).getComprobante().getClave());
  }

  @Test
  public void siUnGastoEsInvalidoNoDebeSubirElArchivo() {
    when(almacen.disponible()).thenReturn(true);
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a 0"))
      .when(gastoServicio)
      .registrarGastos(anyList(), eq(usuario));

    assertThrows(
      GastoInvalidoExeption.class,
      () -> servicio.registrarGastos(List.of(gasto(-1.0)), archivo, usuario)
    );

    verify(almacen, never()).guardar(any(), any(), any());
  }

  @Test
  public void alRecargarUnComprobanteSinGastosDebeReusarloSinCrearOtro() {
    Comprobante anterior = new Comprobante(
      usuario,
      archivo.huella(),
      "image/png",
      AHORA.minusDays(10)
    );
    when(repositorio.buscar(usuario, archivo.huella())).thenReturn(anterior);
    when(repositorio.contarGastos(anterior)).thenReturn(0L);
    List<Gasto> gastos = List.of(gasto(6300.0));

    servicio.registrarGastos(gastos, archivo, usuario);

    verify(repositorio, never()).guardar(any());
    assertSame(anterior, gastos.get(0).getComprobante());
    assertEquals(AHORA, anterior.getFechaCarga());
  }

  @Test
  public void noDebeGuardarGastosDeUnComprobanteYaCargado() {
    Comprobante anterior = new Comprobante(usuario, archivo.huella(), "image/png", AHORA);
    when(repositorio.buscar(usuario, archivo.huella())).thenReturn(anterior);
    when(repositorio.contarGastos(anterior)).thenReturn(1L);

    assertThrows(
      ComprobanteYaCargado.class,
      () -> servicio.registrarGastos(List.of(gasto(6300.0)), archivo, usuario)
    );

    verify(gastoServicio, never()).registrarGastos(anyList(), any());
  }

  private Gasto gasto(Double importe) {
    return new Gasto(importe, LocalDate.of(2026, 11, 18), "Supermercado");
  }
}
