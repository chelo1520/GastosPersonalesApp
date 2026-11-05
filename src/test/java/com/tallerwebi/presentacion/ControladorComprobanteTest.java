package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible;
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

public class ControladorComprobanteTest {

  private GastoServicio gastoServicio;
  private LectorComprobante lectorComprobante;
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
    controladorComprobante =
      new ControladorComprobante(gastoServicio, categoriaRepositorio, lectorComprobante);

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

    ModelAndView resultado = controladorComprobante.procesarComprobante(imagen, request);

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

    ModelAndView resultado = controladorComprobante.procesarComprobante(archivo, request);

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

    ModelAndView resultado = controladorComprobante.procesarComprobante(imagen, request);

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

    ModelAndView resultado = controladorComprobante.procesarComprobante(imagen, request);

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
      controladorComprobante.procesarComprobante(imagen, request)
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

    ModelAndView resultado = controladorComprobante.procesarComprobante(imagen, request);

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

    ModelAndView resultado = controladorComprobante.procesarComprobante(imagen, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("cargá el gasto a mano"));
  }

  @Test
  public void debeMostrarUnaAlternativaManualCuandoNoEstaConfiguradoElServicioDeLectura() {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);
    when(lectorComprobante.leer(png, "image/png", List.of("Alimentación")))
      .thenThrow(new LectorComprobanteNoDisponible());

    ModelAndView resultado = controladorComprobante.procesarComprobante(imagen, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertTrue(((String) resultado.getModel().get("error")).contains("cargar el gasto a mano"));
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
