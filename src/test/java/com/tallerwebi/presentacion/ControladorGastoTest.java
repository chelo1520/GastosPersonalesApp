package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorGastoTest {

  private GastoServicio gastoServicio;
  private CategoriaRepositorio categoriaRepositorio;
  private ServicioPresupuesto servicioPresupuesto;
  private LectorComprobante lectorComprobante;
  private ControladorGasto controladorGasto;

  private Usuario usuario;
  private Gasto gasto;
  private Categoria categoria;
  private HttpServletRequest request;
  private HttpSession session;
  private RedirectAttributes redirectAttributes;

  @BeforeEach
  public void init() {
    gastoServicio = mock(GastoServicio.class);
    categoriaRepositorio = mock(CategoriaRepositorio.class);
    servicioPresupuesto = mock(ServicioPresupuesto.class);
    lectorComprobante = mock(LectorComprobante.class);

    controladorGasto =
      new ControladorGasto(
        gastoServicio,
        categoriaRepositorio,
        servicioPresupuesto,
        lectorComprobante
      );
    categoria = new Categoria();
    categoria.setId(1L);
    categoria.setNombre("Alimentación");
    when(categoriaRepositorio.obtenerTodas()).thenReturn(List.of(categoria));
    when(categoriaRepositorio.buscarPorId(1L)).thenReturn(categoria);

    usuario = mock(Usuario.class);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    redirectAttributes = new RedirectAttributesModelMap();
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);

    gasto = new Gasto(15000.0, LocalDate.of(2026, 9, 26), "Supermercado");
    gasto.setCategoria(categoria);
  }

  @Test
  public void debeMostrarLaPantallaDeCargaDeComprobanteAlUsuarioAutenticado() {
    ModelAndView resultado = controladorGasto.mostrarCargaComprobante(request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals("Desde comprobante", resultado.getModel().get("titulo"));
  }

  @Test
  public void debeRedirigirALoginCuandoSeIntentaCargarUnComprobanteSinSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarCargaComprobante(request).getViewName()
    );
  }

  @Test
  public void debeRechazarUnaImagenQueNoSeaJpgNiPng() {
    MockMultipartFile imagen = new MockMultipartFile(
      "imagen",
      "comprobante.gif",
      "image/gif",
      new byte[] { 1, 2, 3, 4 }
    );

    ModelAndView resultado = controladorGasto.procesarComprobante(imagen, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals(
      "El archivo debe ser una imagen JPG o PNG de hasta 5 MB.",
      resultado.getModel().get("error")
    );
    verifyNoInteractions(lectorComprobante);
  }

  @Test
  public void debeRechazarUnaImagenQueSupereLos5Mb() {
    byte[] contenido = new byte[5 * 1024 * 1024 + 1];
    MockMultipartFile imagen = new MockMultipartFile(
      "imagen",
      "comprobante.png",
      "image/png",
      contenido
    );

    ModelAndView resultado = controladorGasto.procesarComprobante(imagen, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals(
      "La imagen supera los 5 MB. Elegí otra imagen.",
      resultado.getModel().get("error")
    );
    verifyNoInteractions(lectorComprobante);
  }

  @Test
  public void debeMostrarLosDatosExtraidosParaRevisarAntesDeGuardar() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "comprobante.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenReturn(
        new DatosComprobante(
          12500.0,
          LocalDate.of(2026, 10, 12),
          "Almacén",
          "Alimentación",
          "Compra",
          "ticket",
          new ConfianzaComprobante(false, false, false, false)
        )
      );

    ModelAndView resultado = controladorGasto.procesarComprobante(imagen, request);

    assertEquals("revisar-comprobante", resultado.getViewName());
    Gasto gastoPropuesto = (Gasto) resultado.getModel().get("gasto");
    assertEquals(12500.0, gastoPropuesto.getImporte());
    assertEquals(LocalDate.of(2026, 10, 12), gastoPropuesto.getFecha());
    assertEquals("Almacén", gastoPropuesto.getDescripcion());
    assertEquals(categoria, gastoPropuesto.getCategoria());
    assertEquals(
      "Compra",
      ((DatosComprobante) resultado.getModel().get("datosComprobante")).tipoOperacion()
    );
    assertEquals(false, resultado.getModel().get("categoriaBajaConfianza"));
  }

  @Test
  public void debePermitirElegirLaCategoriaCuandoLaSugeridaNoCoincideConUnaExistente() {
    byte[] jpg = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
    MockMultipartFile imagen = new MockMultipartFile(
      "imagen",
      "comprobante.jpg",
      "image/jpeg",
      jpg
    );
    when(lectorComprobante.leer(jpg, "image/jpeg", List.of("Alimentación")))
      .thenReturn(
        new DatosComprobante(
          12500.0,
          LocalDate.of(2026, 10, 12),
          "Almacén",
          "Categoría inventada",
          "Compra",
          "ticket",
          new ConfianzaComprobante(false, false, false, false)
        )
      );

    ModelAndView resultado = controladorGasto.procesarComprobante(imagen, request);

    assertNull(((Gasto) resultado.getModel().get("gasto")).getCategoria());
    assertEquals(true, resultado.getModel().get("categoriaBajaConfianza"));
  }

  @Test
  public void debeOfrecerCargaManualCuandoClaudeNoPuedeLeerLaImagen() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenThrow(new ComprobanteNoLegible());

    ModelAndView resultado = controladorGasto.procesarComprobante(imagen, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("cargá el gasto a mano"));
  }

  @Test
  public void debeMostrarUnaAlternativaManualCuandoNoEstaConfiguradoElServicioDeLectura() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenThrow(new LectorComprobanteNoDisponible());

    ModelAndView resultado = controladorGasto.procesarComprobante(imagen, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("cargar el gasto a mano"));
  }

  @Test
  public void debeMostrarLosGastosOrdenadosConTotal20000CuandoElUsuarioTieneGastosDe15000Y5000() {
    Gasto gastoAnterior = new Gasto(5000.0, LocalDate.of(2026, 9, 20), "Farmacia");
    List<Gasto> gastos = List.of(gastoAnterior, gasto);

    when(gastoServicio.obtenerGastos(usuario)).thenReturn(gastos);

    ModelAndView resultado = controladorGasto.mostrarGastos(request);

    assertEquals("mostrar-gastos", resultado.getViewName());

    assertEquals(List.of(gasto, gastoAnterior), resultado.getModel().get("gastos"));
    assertEquals(20000.0, resultado.getModel().get("total"));

    verify(gastoServicio).obtenerGastos(usuario);
  }

  @Test
  public void debeRegistrarElGastoYRedirigirAMisGastosCuandoElGastoEsValido() {
    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());

    verify(gastoServicio).registrarGasto(gasto, usuario);
  }

  @Test
  public void debeMandarLaAlertaDeRepeticionALaListaCuandoElGastoRegistradoLaDispara() {
    Alerta alerta = new Alerta(
      Alerta.Tipo.REPETICION,
      Alerta.Nivel.AMARILLA,
      "Es tu 4.º gasto en Café y snacks esta semana ($10.000)",
      1L
    );
    when(gastoServicio.alertaDeRepeticion(gasto, usuario)).thenReturn(alerta);

    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
    assertEquals(alerta, redirectAttributes.getFlashAttributes().get("alertaRepeticion"));
  }

  @Test
  public void debeMandarLaAlertaDePresupuestoAlPopUpCuandoElGastoRegistradoCruzaEl80PorCiento() {
    Alerta alerta = new Alerta(
      Alerta.Tipo.PRESUPUESTO,
      Alerta.Nivel.AMARILLA,
      "Con este gasto llegaste al 80% del presupuesto del mes ($440.000 de $550.000)",
      null
    );
    when(gastoServicio.alertaDePresupuesto(gasto, usuario)).thenReturn(alerta);

    controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertEquals(alerta, redirectAttributes.getFlashAttributes().get("alertaPresupuesto"));
  }

  @Test
  public void noDebeMandarAlertaCuandoElGastoRegistradoNoLaDispara() {
    controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertTrue(redirectAttributes.getFlashAttributes().isEmpty());
  }

  @Test
  public void debeMostrarElFormularioConLaFechaDeHoyCuandoSeEntraARegistrarGasto() {
    ModelAndView resultado = controladorGasto.mostrarFormularioRegistrarGasto(request);

    assertEquals("registrar-gasto", resultado.getViewName());

    Gasto gastoDelFormulario = (Gasto) resultado.getModel().get("gasto");
    assertEquals(LocalDate.now(), gastoDelFormulario.getFecha());
    assertEquals(List.of(categoria), resultado.getModel().get("categorias"));
  }

  @Test
  public void debeRechazarLaCategoriaInexistenteYVolverAlFormulario() {
    Categoria categoriaInexistente = new Categoria();
    categoriaInexistente.setId(99L);
    gasto.setCategoria(categoriaInexistente);

    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("La categoría seleccionada no existe", resultado.getModel().get("error"));
    verify(gastoServicio, never()).registrarGasto(gasto, usuario);
  }

  @Test
  public void debePedirUnaCategoriaCuandoNoSeSeleccionaNinguna() {
    gasto.setCategoria(null);

    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("Seleccioná una categoría", resultado.getModel().get("error"));
    verify(gastoServicio, never()).registrarGasto(gasto, usuario);
  }

  @Test
  public void debeLimitarElCalendarioAlMesActualCuandoSeEntraARegistrarGasto() {
    when(gastoServicio.obtenerFechaMinimaPermitida()).thenReturn(LocalDate.of(2026, 10, 1));
    when(gastoServicio.obtenerFechaMaximaPermitida()).thenReturn(LocalDate.of(2026, 10, 31));

    ModelAndView resultado = controladorGasto.mostrarFormularioRegistrarGasto(request);

    assertEquals(LocalDate.of(2026, 10, 1), resultado.getModel().get("fechaMinima"));
    assertEquals(LocalDate.of(2026, 10, 31), resultado.getModel().get("fechaMaxima"));
  }

  @Test
  public void debeVolverAlFormularioConElMensajeDeErrorCuandoElServicioRechazaElGasto() {
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a cero"))
      .when(gastoServicio)
      .registrarGasto(gasto, usuario);

    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request, redirectAttributes);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("El importe debe ser mayor a cero", resultado.getModel().get("error"));
    assertEquals(gasto, resultado.getModel().get("gasto"));
  }

  @Test
  public void debeMostrarElFormularioPrecargadoConElGastoCuandoSeEntraAModificarGasto() {
    when(gastoServicio.obtenerGasto(10L, usuario)).thenReturn(gasto);

    ModelAndView resultado = controladorGasto.mostrarFormularioModificarGasto(10L, request);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals(gasto, resultado.getModel().get("gasto"));
    assertEquals("Modificar gasto", resultado.getModel().get("titulo"));
    assertEquals("/modificar-gasto/10", resultado.getModel().get("accion"));
    assertEquals("/mostrar-gastos", resultado.getModel().get("urlCancelar"));
  }

  @Test
  public void debeRedirigirAMisGastosCuandoSeQuiereModificarUnGastoInexistenteODeOtroUsuario() {
    when(gastoServicio.obtenerGasto(99L, usuario)).thenThrow(new GastoNoEncontrado());

    ModelAndView resultado = controladorGasto.mostrarFormularioModificarGasto(99L, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeModificarElGastoYRedirigirAMisGastosCuandoLosDatosSonValidos() {
    ModelAndView resultado = controladorGasto.modificarGasto(10L, gasto, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());

    verify(gastoServicio).modificarGasto(10L, gasto, usuario);
  }

  @Test
  public void debeVolverAlFormularioDeModificarConElErrorCuandoElServicioRechazaLosDatos() {
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a cero"))
      .when(gastoServicio)
      .modificarGasto(10L, gasto, usuario);

    ModelAndView resultado = controladorGasto.modificarGasto(10L, gasto, request);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("El importe debe ser mayor a cero", resultado.getModel().get("error"));
    assertEquals(gasto, resultado.getModel().get("gasto"));
    assertEquals("/modificar-gasto/10", resultado.getModel().get("accion"));
  }

  @Test
  public void debeEliminarElGastoYRedirigirAMisGastos() {
    ModelAndView resultado = controladorGasto.eliminarGasto(10L, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
    verify(gastoServicio).eliminarGasto(10L, usuario);
  }

  @Test
  public void debeRedirigirAMisGastosCuandoSeQuiereEliminarUnGastoInexistenteODeOtroUsuario() {
    doThrow(new GastoNoEncontrado()).when(gastoServicio).eliminarGasto(99L, usuario);

    ModelAndView resultado = controladorGasto.eliminarGasto(99L, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeRedirigirALoginSinEliminarCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals("redirect:/login", controladorGasto.eliminarGasto(10L, request).getViewName());

    verifyNoInteractions(gastoServicio);
  }

  @Test
  public void debeRedirigirALoginSinModificarCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarFormularioModificarGasto(10L, request).getViewName()
    );
    assertEquals(
      "redirect:/login",
      controladorGasto.modificarGasto(10L, gasto, request).getViewName()
    );

    verifyNoInteractions(gastoServicio);
  }

  @Test
  public void debeMostrarTotal45000CuandoLosGastosEntreLasFechasSuman45000() {
    LocalDate desde = LocalDate.of(2026, 9, 1);
    LocalDate hasta = LocalDate.of(2026, 9, 30);

    when(gastoServicio.sumarGastos(usuario, desde, hasta)).thenReturn(45000.0);

    ModelAndView resultado = controladorGasto.sumarGastos(request, desde, hasta);

    assertEquals("sumar-gastos", resultado.getViewName());

    assertEquals(45000.0, resultado.getModel().get("total"));

    verify(gastoServicio).sumarGastos(usuario, desde, hasta);
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals("redirect:/login", controladorGasto.mostrarGastos(request).getViewName());
    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarFormularioRegistrarGasto(request).getViewName()
    );
    assertEquals(
      "redirect:/login",
      controladorGasto.registrarGasto(gasto, request, redirectAttributes).getViewName()
    );
    assertEquals(
      "redirect:/login",
      controladorGasto
        .sumarGastos(request, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        .getViewName()
    );

    verifyNoInteractions(gastoServicio);
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioCuandoNoExisteLaSesion() {
    when(request.getSession(false)).thenReturn(null);

    assertEquals("redirect:/login", controladorGasto.mostrarGastos(request).getViewName());
    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarFormularioRegistrarGasto(request).getViewName()
    );
    assertEquals(
      "redirect:/login",
      controladorGasto.registrarGasto(gasto, request, redirectAttributes).getViewName()
    );
    assertEquals(
      "redirect:/login",
      controladorGasto
        .sumarGastos(request, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        .getViewName()
    );

    verifyNoInteractions(gastoServicio);
  }

  @Test
  public void debeUsarElPresupuestoDelMesSimuladoCuandoEstaDefinido() {
    YearMonth mesActual = YearMonth.now();
    when(servicioPresupuesto.totalDelMes(usuario, mesActual.plusMonths(1))).thenReturn(480000.0);
    when(servicioPresupuesto.totalDelMes(usuario, mesActual)).thenReturn(400000.0);

    ModelAndView mav = controladorGasto.mostrarSimulacion(request);

    assertEquals(480000.0, mav.getModel().get("presupuesto"));
  }

  @Test
  public void debeUsarElPresupuestoDelMesActualCuandoElMesSimuladoNoTieneNingunoDefinido() {
    YearMonth mesActual = YearMonth.now();
    when(servicioPresupuesto.totalDelMes(usuario, mesActual.plusMonths(1))).thenReturn(0.0);
    when(servicioPresupuesto.totalDelMes(usuario, mesActual)).thenReturn(400000.0);

    ModelAndView mav = controladorGasto.mostrarSimulacion(request);

    assertEquals(400000.0, mav.getModel().get("presupuesto"));
  }
}
