package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.comprobante.ArchivoComprobante;
import com.tallerwebi.dominio.comprobante.ComprobanteNoEncontrado;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ComprobanteYaCargado;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible;
import com.tallerwebi.dominio.comprobante.ServicioComprobante;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;

public class ControladorComprobanteTest {

  private GastoServicio gastoServicio;
  private LectorComprobante lectorComprobante;
  private ServicioComprobante servicioComprobante;
  private ControladorComprobante controladorComprobante;

  private Usuario usuario;
  private Categoria categoria;
  private HttpServletRequest request;
  private HttpSession session;
  private RedirectAttributes redirectAttributes;

  @BeforeEach
  public void init() {
    gastoServicio = mock(GastoServicio.class);
    CategoriaRepositorio categoriaRepositorio = mock(CategoriaRepositorio.class);
    lectorComprobante = mock(LectorComprobante.class);
    servicioComprobante = mock(ServicioComprobante.class);
    controladorComprobante =
      new ControladorComprobante(
        gastoServicio,
        categoriaRepositorio,
        lectorComprobante,
        servicioComprobante
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
    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);
  }

  @Test
  public void debeMostrarLaPantallaDeCargaDeComprobanteAlUsuarioAutenticado() {
    ModelAndView resultado = controladorComprobante.mostrarCargaComprobante(request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals("Desde comprobante", resultado.getModel().get("titulo"));
  }

  @Test
  public void debeRedirigirALoginCuandoSeIntentaCargarUnComprobanteSinSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals(
      "redirect:/login",
      controladorComprobante.mostrarCargaComprobante(request).getViewName()
    );
  }

  @Test
  public void debeRechazarUnArchivoQueNoSeaJpgPngNiPdf() {
    MockMultipartFile imagen = new MockMultipartFile(
      "imagen",
      "comprobante.gif",
      "image/gif",
      new byte[] { 1, 2, 3, 4 }
    );

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals(
      "El archivo debe ser JPG, PNG o PDF de hasta 5 MB.",
      resultado.getModel().get("error")
    );
    verifyNoInteractions(lectorComprobante);
  }

  @Test
  public void debeAceptarUnPdfYMandarloALeerComoTal() {
    byte[] pdf = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x35 };
    MockMultipartFile archivo = new MockMultipartFile(
      "imagen",
      "comprobante.pdf",
      "application/pdf",
      pdf
    );
    when(lectorComprobante.leer(pdf, "application/pdf", List.of("Alimentación")))
      .thenReturn(datosDeEjemplo());

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      archivo,
      request,
      redirectAttributes
    );

    assertEquals("revisar-comprobante", resultado.getViewName());
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

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals("El archivo supera los 5 MB. Elegí otro.", resultado.getModel().get("error"));
    verifyNoInteractions(lectorComprobante);
  }

  @Test
  public void debeMostrarLosDatosExtraidosParaRevisarAntesDeGuardar() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "comprobante.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenReturn(
        comprobante(
          new GastoDelComprobante(
            12500.0,
            LocalDate.of(2026, 10, 12),
            "Almacén",
            null,
            "Alimentación",
            new ConfianzaComprobante(false, false, false, false)
          )
        )
      );

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("revisar-comprobante", resultado.getViewName());
    ConfirmacionComprobante confirmacion = confirmacionDe(resultado);
    assertEquals("Compra", confirmacion.getTipoOperacion());
    GastoPropuesto propuesto = confirmacion.getGastos().get(0);
    assertEquals(12500.0, propuesto.getImporte());
    assertEquals(LocalDate.of(2026, 10, 12), propuesto.getFecha());
    assertEquals("Almacén", propuesto.getDescripcion());
    assertEquals(1L, propuesto.getCategoriaId());
    assertTrue(propuesto.isIncluir());
    assertFalse(propuesto.isRevisarCategoria());
  }

  @Test
  public void debeProponerUnGastoPorCadaCategoriaDelTicket() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenReturn(
        comprobante(
          gastoLeido(6300.0, "Leche, pan", "Alimentación"),
          gastoLeido(3500.0, "Caramelos, alfajores", "Café y snacks")
        )
      );

    ConfirmacionComprobante confirmacion = confirmacionDe(
      controladorComprobante.procesarComprobante(imagen, request, redirectAttributes)
    );

    assertEquals(2, confirmacion.getGastos().size());
    assertEquals("Leche, pan", confirmacion.getGastos().get(0).getDetalle());
    assertEquals(1L, confirmacion.getGastos().get(0).getCategoriaId());
    // "Café y snacks" no está entre las categorías del test: la elige la persona
    assertNull(confirmacion.getGastos().get(1).getCategoriaId());
    assertTrue(confirmacion.getGastos().get(1).isRevisarCategoria());
  }

  @Test
  public void debeGuardarSoloLosGastosTildadosDelComprobante() {
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true),
      propuesto(3500.0, "Supermercado", 1L, false),
      propuesto(8000.0, "Peluquería", 1L, true)
    );

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Gasto>> guardados = ArgumentCaptor.forClass(List.class);
    verify(gastoServicio).registrarGastos(guardados.capture(), eq(usuario));
    assertEquals(2, guardados.getValue().size());
    assertEquals(6300.0, guardados.getValue().get(0).getImporte());
    assertEquals("Peluquería", guardados.getValue().get(1).getDescripcion());
    assertEquals(categoria, guardados.getValue().get(1).getCategoria());
    // Varios gastos juntos: sin alerta de repetición (se dispararían entre sí)
    verify(gastoServicio, never()).alertaDeRepeticion(any(), any());
    verify(gastoServicio).alertaDePresupuesto(guardados.getValue(), usuario);
  }

  // Desde el registro rápido (modal del resumen o de Mis gastos)

  @Test
  public void alConfirmarDesdeElResumenDebeVolverAlResumenConElAvisoDeLosGastosGuardados() {
    when(request.getParameter("volver")).thenReturn("resumen");
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true),
      propuesto(8000.0, "Peluquería", 1L, true)
    );

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("redirect:/home", resultado.getViewName());
    assertEquals(
      "Guardaste 2 gastos del comprobante",
      redirectAttributes.getFlashAttributes().get("avisoRegistro")
    );
  }

  @Test
  public void alConfirmarUnSoloGastoDesdeMisGastosDebeVolverAMisGastosConElAviso() {
    when(request.getParameter("volver")).thenReturn("mis-gastos");
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true)
    );

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
    assertEquals(
      "Guardaste 1 gasto del comprobante",
      redirectAttributes.getFlashAttributes().get("avisoRegistro")
    );
  }

  @Test
  public void sinOrigenAlConfirmarDebeIrAMisGastosSinAvisoComoAntes() {
    controladorComprobante.confirmarComprobante(
      confirmacionCon(propuesto(6300.0, "Supermercado", 1L, true)),
      request,
      redirectAttributes
    );

    assertNull(redirectAttributes.getFlashAttributes().get("avisoRegistro"));
  }

  @Test
  public void laRevisionDebeRecordarDesdeDondeSeSubioElComprobante() {
    when(request.getParameter("volver")).thenReturn("resumen");
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenReturn(comprobante(gastoLeido(6300.0, "Leche, pan", "Alimentación")));

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("revisar-comprobante", resultado.getViewName());
    assertEquals("resumen", resultado.getModel().get("volver"));
  }

  @Test
  public void unOrigenDesconocidoNoDebeLlegarALaRevision() {
    when(request.getParameter("volver")).thenReturn("https://otro-sitio.com");

    ModelAndView resultado = controladorComprobante.mostrarCargaComprobante(request);

    assertNull(resultado.getModel().get("volver"));
  }

  @Test
  public void debeVolverALaRevisionSiNoHayNingunGastoTildado() {
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, false)
    );

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("revisar-comprobante", resultado.getViewName());
    assertEquals("Elegí al menos un gasto para guardar.", resultado.getModel().get("error"));
    verify(gastoServicio, never()).registrarGastos(any(), any());
  }

  @Test
  public void debeVolverALaRevisionSiUnGastoTildadoNoTieneCategoria() {
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true),
      propuesto(3500.0, "Kiosco", null, true)
    );

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("revisar-comprobante", resultado.getViewName());
    assertEquals("Elegí una categoría para \"Kiosco\".", resultado.getModel().get("error"));
    assertSame(confirmacion, resultado.getModel().get("confirmacion"));
    verify(gastoServicio, never()).registrarGastos(any(), any());
  }

  @Test
  public void debeVolverALaRevisionConElErrorSiUnGastoEsInvalido() {
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true)
    );
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a cero"))
      .when(gastoServicio)
      .registrarGastos(any(), eq(usuario));

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("revisar-comprobante", resultado.getViewName());
    assertEquals("El importe debe ser mayor a cero", resultado.getModel().get("error"));
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
      .thenReturn(comprobante(gastoLeido(12500.0, null, "Categoría inventada")));

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    GastoPropuesto propuesto = confirmacionDe(resultado).getGastos().get(0);
    assertNull(propuesto.getCategoriaId());
    assertTrue(propuesto.isRevisarCategoria());
  }

  @Test
  public void debeOfrecerCargaManualCuandoClaudeNoPuedeLeerLaImagen() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenThrow(new ComprobanteNoLegible());

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("cargá el gasto a mano"));
  }

  @Test
  public void debeMostrarUnaAlternativaManualCuandoNoEstaConfiguradoElServicioDeLectura() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenThrow(new LectorComprobanteNoDisponible());

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("cargar el gasto a mano"));
  }

  // El archivo del comprobante (R2 y repetidos)

  @Test
  public void alLeerElComprobanteDebeDejarElArchivoEnLaSesionYSuHuellaEnLaRevision() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenReturn(datosDeEjemplo());

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    ArgumentCaptor<ArchivoComprobante> archivo = ArgumentCaptor.forClass(ArchivoComprobante.class);
    verify(session).setAttribute(eq("comprobantePendiente"), archivo.capture());
    assertEquals("image/png", archivo.getValue().tipo());
    assertEquals(archivo.getValue().huella(), confirmacionDe(resultado).getHuella());
  }

  @Test
  public void unComprobanteYaCargadoNoDebeMandarseALeer() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    doThrow(new ComprobanteYaCargado(LocalDate.of(2026, 11, 12), 3))
      .when(servicioComprobante)
      .verificarQueNoEsteCargado(any(), eq(usuario));

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(
      ((String) resultado.getModel().get("error")).startsWith(
          "Este comprobante ya lo cargaste el 12/11/2026 (3 gastos)."
        )
    );
    verifyNoInteractions(lectorComprobante);
  }

  @Test
  public void alConfirmarDebeGuardarLosGastosConElArchivoQueSeLeyo() {
    ArchivoComprobante archivo = ArchivoComprobante.de(new byte[] { 1, 2, 3 }, "image/png");
    when(session.getAttribute("comprobantePendiente")).thenReturn(archivo);
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true)
    );
    confirmacion.setHuella(archivo.huella());

    controladorComprobante.confirmarComprobante(confirmacion, request, redirectAttributes);

    verify(servicioComprobante).registrarGastos(anyList(), eq(archivo), eq(usuario));
    verify(gastoServicio, never()).registrarGastos(anyList(), any());
    verify(session).removeAttribute("comprobantePendiente");
  }

  @Test
  public void siElArchivoDeLaSesionEsDeOtraRevisionDebeGuardarLosGastosSinArchivo() {
    ArchivoComprobante otro = ArchivoComprobante.de(new byte[] { 9, 9 }, "image/png");
    when(session.getAttribute("comprobantePendiente")).thenReturn(otro);
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true)
    );
    confirmacion.setHuella("huella-de-otro-archivo");

    controladorComprobante.confirmarComprobante(confirmacion, request, redirectAttributes);

    verify(gastoServicio).registrarGastos(anyList(), eq(usuario));
    verifyNoInteractions(servicioComprobante);
  }

  @Test
  public void siElComprobanteSeCargoMientrasSeRevisabaDebeAvisarYNoGuardar() {
    ArchivoComprobante archivo = ArchivoComprobante.de(new byte[] { 1, 2, 3 }, "image/png");
    when(session.getAttribute("comprobantePendiente")).thenReturn(archivo);
    doThrow(new ComprobanteYaCargado(LocalDate.of(2026, 11, 12), 1))
      .when(servicioComprobante)
      .registrarGastos(anyList(), eq(archivo), eq(usuario));
    ConfirmacionComprobante confirmacion = confirmacionCon(
      propuesto(6300.0, "Supermercado", 1L, true)
    );
    confirmacion.setHuella(archivo.huella());

    ModelAndView resultado = controladorComprobante.confirmarComprobante(
      confirmacion,
      request,
      redirectAttributes
    );

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("(1 gasto)"));
    verify(session).removeAttribute("comprobantePendiente");
  }

  @Test
  public void desdeElModalUnComprobanteRepetidoDebeVolverAlModalConElMotivo() {
    when(request.getParameter("volver")).thenReturn("resumen");
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    doThrow(new ComprobanteYaCargado(LocalDate.of(2026, 11, 12), 1))
      .when(servicioComprobante)
      .verificarQueNoEsteCargado(any(), eq(usuario));

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      imagen,
      request,
      redirectAttributes
    );

    assertEquals("redirect:/home", resultado.getViewName());
    assertTrue(
      ((String) redirectAttributes.getFlashAttributes().get("errorComprobante")).startsWith(
          "Este comprobante ya lo cargaste el 12/11/2026 (1 gasto)."
        )
    );
  }

  @Test
  public void desdeElModalUnArchivoInvalidoDebeVolverAlModalDeMisGastos() {
    when(request.getParameter("volver")).thenReturn("mis-gastos");
    MockMultipartFile gif = new MockMultipartFile(
      "imagen",
      "comprobante.gif",
      "image/gif",
      new byte[] { 1, 2, 3, 4 }
    );

    ModelAndView resultado = controladorComprobante.procesarComprobante(
      gif,
      request,
      redirectAttributes
    );

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
    assertEquals(
      "El archivo debe ser JPG, PNG o PDF de hasta 5 MB.",
      redirectAttributes.getFlashAttributes().get("errorComprobante")
    );
  }

  // Ver el archivo

  @Test
  public void verElComprobanteDebeRedirigirAlLinkTemporalSinTocarlo() {
    String link = "https://cuenta.r2.cloudflarestorage.com/b/x.png?X-Amz-Signature=a%2Bb";
    when(servicioComprobante.urlDelArchivo(3L, usuario)).thenReturn(link);

    ModelAndView resultado = controladorComprobante.verComprobante(3L, request, redirectAttributes);

    RedirectView vista = (RedirectView) resultado.getView();
    assertEquals(link, vista.getUrl());
  }

  @Test
  public void siElComprobanteNoEstaDisponibleDebeVolverAMisGastosConElAviso() {
    when(servicioComprobante.urlDelArchivo(3L, usuario)).thenThrow(new ComprobanteNoEncontrado());

    ModelAndView resultado = controladorComprobante.verComprobante(3L, request, redirectAttributes);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
    assertEquals(
      "El archivo de este comprobante no está disponible.",
      redirectAttributes.getFlashAttributes().get("avisoRegistro")
    );
  }

  @Test
  public void sinSesionNoSeVeNingunComprobante() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorComprobante.verComprobante(3L, request, redirectAttributes);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(servicioComprobante);
  }

  private DatosComprobante datosDeEjemplo() {
    return comprobante(gastoLeido(49999.0, null, "Alimentación"));
  }

  // El total es la suma de los gastos, como en un comprobante bien leído
  private DatosComprobante comprobante(GastoDelComprobante... gastos) {
    double total = 0;
    for (GastoDelComprobante gastoLeido : gastos) {
      total += gastoLeido.importe();
    }
    return new DatosComprobante("Compra", "ticket", total, List.of(gastos));
  }

  private GastoDelComprobante gastoLeido(Double importe, String detalle, String categoria) {
    return new GastoDelComprobante(
      importe,
      LocalDate.of(2026, 10, 15),
      "Supermercado",
      detalle,
      categoria,
      new ConfianzaComprobante(false, false, false, false)
    );
  }

  private ConfirmacionComprobante confirmacionDe(ModelAndView vista) {
    return (ConfirmacionComprobante) vista.getModel().get("confirmacion");
  }

  private ConfirmacionComprobante confirmacionCon(GastoPropuesto... gastos) {
    ConfirmacionComprobante confirmacion = new ConfirmacionComprobante();
    confirmacion.setGastos(new java.util.ArrayList<>(List.of(gastos)));
    return confirmacion;
  }

  private GastoPropuesto propuesto(
    Double importe,
    String descripcion,
    Long categoriaId,
    boolean incluir
  ) {
    GastoPropuesto propuesto = new GastoPropuesto();
    propuesto.setImporte(importe);
    propuesto.setFecha(LocalDate.of(2026, 10, 15));
    propuesto.setDescripcion(descripcion);
    propuesto.setCategoriaId(categoriaId);
    propuesto.setIncluir(incluir);
    return propuesto;
  }
}
