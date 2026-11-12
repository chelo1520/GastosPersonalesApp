package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.ServicioCuenta;
import com.tallerwebi.dominio.ServicioGuia;
import com.tallerwebi.dominio.ServicioPerfil;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import com.tallerwebi.dominio.excepcion.DatosPersonalesInvalidos;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.MesDePresupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPerfilTest {

  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private Usuario usuarioMock;
  private ServicioCuenta servicioCuentaMock;
  private ServicioPerfil servicioPerfilMock;
  private ServicioPresupuesto servicioPresupuestoMock;
  private ServicioGuia servicioGuiaMock;
  private ControladorPerfil controladorPerfil;

  @BeforeEach
  public void init() {
    this.servicioCuentaMock = mock(ServicioCuenta.class);
    this.servicioPerfilMock = mock(ServicioPerfil.class);
    this.servicioPresupuestoMock = mock(ServicioPresupuesto.class);
    this.servicioGuiaMock = mock(ServicioGuia.class);
    this.controladorPerfil =
      new ControladorPerfil(
        servicioCuentaMock,
        servicioPerfilMock,
        servicioPresupuestoMock,
        servicioGuiaMock
      );
    this.usuarioMock = mock(Usuario.class);
    this.requestMock = mock(HttpServletRequest.class);
    this.sessionMock = mock(HttpSession.class);
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO")).thenReturn(usuarioMock);
  }

  @Test
  public void cuandoHayUnUsuarioAuntenticadoEnLaSesionMuestraElPerfilYRetornaUsuarioALaVista() {
    ModelAndView resultado = controladorPerfil.mostrarPerfil(null, null, requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertSame(usuarioMock, resultado.getModel().get("usuario"));
  }

  @Test
  public void cuandoNoHayUsuarioEnLaSesionRedirigeAlLogin() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorPerfil.mostrarPerfil(null, null, requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
  }

  @Test
  public void cuandoNoExisteLaSesionRedirigeAlLogin() {
    when(requestMock.getSession(false)).thenReturn(null);

    ModelAndView resultado = controladorPerfil.mostrarPerfil(null, null, requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
  }

  @Test
  public void debeCambiarLaContrasenaActualizarLaSesionYVolverAlPerfilConElAviso() {
    Usuario actualizado = new Usuario();
    when(servicioCuentaMock.cambiarContrasena(usuarioMock, "Actual1!", "Nueva123!"))
      .thenReturn(actualizado);

    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Actual1!",
      "Nueva123!",
      "Nueva123!",
      requestMock
    );

    assertEquals("redirect:/perfil?contrasenaCambiada", resultado.getViewName());
    verify(sessionMock).setAttribute("USUARIO", actualizado);
  }

  @Test
  public void noDebeCambiarlaCuandoLasContrasenasNuevasNoCoinciden() {
    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Actual1!",
      "Nueva123!",
      "Otra1234!",
      requestMock
    );

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "Las contraseñas nuevas no coinciden",
      resultado.getModel().get("errorContrasena")
    );
    verify(servicioCuentaMock, never()).cambiarContrasena(any(), any(), any());
  }

  @Test
  public void debeAvisarCuandoLaContrasenaActualEsIncorrecta() {
    when(servicioCuentaMock.cambiarContrasena(usuarioMock, "Mal1234!", "Nueva123!"))
      .thenThrow(new ContrasenaIncorrecta());

    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Mal1234!",
      "Nueva123!",
      "Nueva123!",
      requestMock
    );

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "La contraseña actual no es correcta",
      resultado.getModel().get("errorContrasena")
    );
    verify(sessionMock, never()).setAttribute(any(), any());
  }

  @Test
  public void debeAvisarLasReglasCuandoLaNuevaNoLasCumple() {
    when(servicioCuentaMock.cambiarContrasena(usuarioMock, "Actual1!", "nueva"))
      .thenThrow(new ContrasenaInvalida());

    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Actual1!",
      "nueva",
      "nueva",
      requestMock
    );

    assertEquals(ReglasDeContrasena.MENSAJE, resultado.getModel().get("errorContrasena"));
  }

  @Test
  public void debeEliminarLaCuentaCerrarLaSesionYVolverAlLoginConElAviso() {
    ModelAndView resultado = controladorPerfil.eliminarCuenta("Actual1!", requestMock);

    verify(servicioCuentaMock).eliminarCuenta(usuarioMock, "Actual1!");
    verify(sessionMock).invalidate();
    assertEquals("redirect:/login?cuentaEliminada", resultado.getViewName());
  }

  @Test
  public void noDebeEliminarNiCerrarLaSesionCuandoLaContrasenaEsIncorrecta() {
    doThrow(new ContrasenaIncorrecta())
      .when(servicioCuentaMock)
      .eliminarCuenta(usuarioMock, "Mal1234!");

    ModelAndView resultado = controladorPerfil.eliminarCuenta("Mal1234!", requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "La contraseña no es correcta: la cuenta no se eliminó",
      resultado.getModel().get("errorEliminar")
    );
    verify(sessionMock, never()).invalidate();
  }

  @Test
  public void noDebeEliminarNadaSinSesion() {
    when(requestMock.getSession(false)).thenReturn(null);

    ModelAndView resultado = controladorPerfil.eliminarCuenta("Actual1!", requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
    verify(servicioCuentaMock, never()).eliminarCuenta(any(), any());
  }

  @Test
  public void debeGuardarElIngresoYElPorcentajeYRedirigirAlResumen() {
    ModelAndView resultado = controladorPerfil.definirPresupuesto(400000.0, 50.0, requestMock);

    verify(servicioPerfilMock).definirPresupuesto(usuarioMock, 400000.0, 50.0);
    verify(servicioGuiaMock, never()).guardarPaso(any(), anyInt());
    assertEquals("redirect:/home?presupuestoGuardado", resultado.getViewName());
  }

  @Test
  public void debeVolverAlPerfilConElErrorCuandoElServicioRechazaLosDatos() {
    when(usuarioMock.getGuiaPaso()).thenReturn(1);
    doThrow(new PresupuestoInvalido("El ingreso debe ser mayor a cero"))
      .when(servicioPerfilMock)
      .definirPresupuesto(eq(usuarioMock), any(), any());

    ModelAndView resultado = controladorPerfil.definirPresupuesto(0.0, 50.0, requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertEquals("El ingreso debe ser mayor a cero", resultado.getModel().get("error"));
    assertSame(usuarioMock, resultado.getModel().get("usuario"));
    verify(servicioGuiaMock, never()).guardarPaso(any(), anyInt());
  }

  @Test
  public void debeAvanzarLaGuiaAlGuardarElPresupuestoEnElPrimerPaso() {
    when(usuarioMock.getGuiaPaso()).thenReturn(1);

    ModelAndView resultado = controladorPerfil.definirPresupuesto(400000.0, 50.0, requestMock);

    verify(servicioPerfilMock).definirPresupuesto(usuarioMock, 400000.0, 50.0);
    verify(servicioGuiaMock).guardarPaso(usuarioMock, 2);
    assertEquals("redirect:/presupuestos", resultado.getViewName());
  }

  @Test
  public void noDebeCambiarLaGuiaAlGuardarElPresupuestoEnOtroPaso() {
    when(usuarioMock.getGuiaPaso()).thenReturn(3);

    ModelAndView resultado = controladorPerfil.definirPresupuesto(400000.0, 50.0, requestMock);

    verify(servicioGuiaMock, never()).guardarPaso(any(), anyInt());
    assertEquals("redirect:/home?presupuestoGuardado", resultado.getViewName());
  }

  @Test
  public void debeRedirigirAlLoginSinGuardarCuandoNoHayUsuarioAlDefinirElPresupuesto() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorPerfil.definirPresupuesto(400000.0, 50.0, requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
    verify(servicioPerfilMock, never()).definirPresupuesto(any(), any(), any());
  }

  @Test
  public void debeMostrarElPresupuestoTotalDelMesElegido() {
    when(servicioPresupuestoMock.presupuestoDelMes(usuarioMock, YearMonth.of(2026, 12)))
      .thenReturn(500000.0);

    ModelAndView resultado = controladorPerfil.mostrarPerfil("2026-12", null, requestMock);

    assertEquals("2026-12", resultado.getModel().get("mesPresupuesto"));
    assertEquals(500000.0, resultado.getModel().get("totalDelMes"));
  }

  @Test
  public void debeDefinirElPresupuestoDeUnMesYLlevarAlResumenDeEseMes() {
    ModelAndView resultado = controladorPerfil.definirPresupuestoDelMes(
      "2026-12",
      500000.0,
      requestMock
    );

    verify(servicioPresupuestoMock)
      .definirPresupuestoDelMes(usuarioMock, YearMonth.of(2026, 12), 500000.0);
    assertEquals("redirect:/home?mes=2026-12&presupuestoGuardado", resultado.getViewName());
  }

  @Test
  public void debeMostrarElErrorSiElTotalDelMesEsMenorAlAsignado() {
    doThrow(new PresupuestoInvalido("El total no puede ser menor"))
      .when(servicioPresupuestoMock)
      .definirPresupuestoDelMes(eq(usuarioMock), any(YearMonth.class), any());

    ModelAndView resultado = controladorPerfil.definirPresupuestoDelMes(
      "2026-12",
      10.0,
      requestMock
    );

    assertEquals("perfil", resultado.getViewName());
    assertEquals("El total no puede ser menor", resultado.getModel().get("errorMes"));
  }

  @Test
  public void debeArmarLaTablaAnualConLosDoceMesesDelAnioElegido() {
    List<MesDePresupuesto> meses = new ArrayList<>();
    for (int numero = 1; numero <= 12; numero++) {
      meses.add(new MesDePresupuesto(YearMonth.of(2027, numero), 500000.0, false, 0.0));
    }
    when(servicioPresupuestoMock.resumenDelAnio(usuarioMock, 2027)).thenReturn(meses);

    ModelAndView resultado = controladorPerfil.mostrarPerfil(null, 2027, requestMock);

    assertEquals(2027, resultado.getModel().get("anio"));
    assertEquals(2026, resultado.getModel().get("anioAnterior"));
    assertEquals(2028, resultado.getModel().get("anioSiguiente"));
    @SuppressWarnings("unchecked")
    List<MesDelAnio> filas = (List<MesDelAnio>) resultado.getModel().get("mesesDelAnio");
    assertEquals(12, filas.size());
    assertEquals("Enero", filas.get(0).getNombre());
    assertEquals("2027-12", filas.get(11).getMes());
    assertEquals(500000.0, filas.get(0).getSinAsignar());
  }

  @Test
  public void sinAnioLaTablaAnualMuestraElAnioDelMesElegido() {
    when(servicioPresupuestoMock.resumenDelAnio(usuarioMock, 2025)).thenReturn(List.of());

    ModelAndView resultado = controladorPerfil.mostrarPerfil("2025-03", null, requestMock);

    assertEquals(2025, resultado.getModel().get("anio"));
    verify(servicioPresupuestoMock).resumenDelAnio(usuarioMock, 2025);
  }

  @Test
  public void debeGuardarLosDatosPersonalesYVolverAlPerfilConAviso() {
    DatosPersonales datos = new DatosPersonales();
    datos.setNombre("Diego");
    datos.setApellido("Villca");
    datos.setFechaNacimiento("1990-05-20");

    ModelAndView resultado = controladorPerfil.actualizarDatosPersonales(datos, requestMock);

    verify(servicioPerfilMock)
      .actualizarDatosPersonales(usuarioMock, "Diego", "Villca", LocalDate.of(1990, 5, 20));
    assertEquals("redirect:/perfil?datosGuardados", resultado.getViewName());
  }

  @Test
  public void debeVolverAlPerfilConElErrorCuandoLaFechaNoEsValida() {
    DatosPersonales datos = new DatosPersonales();
    datos.setNombre("Diego");
    datos.setFechaNacimiento("ayer");

    ModelAndView resultado = controladorPerfil.actualizarDatosPersonales(datos, requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertEquals("La fecha de nacimiento no es válida", resultado.getModel().get("errorDatos"));
    verify(servicioPerfilMock, never()).actualizarDatosPersonales(any(), any(), any(), any());
  }

  @Test
  public void debeMostrarElErrorDelServicioAlGuardarDatosPersonales() {
    DatosPersonales datos = new DatosPersonales();
    datos.setNombre("a".repeat(61));
    doThrow(new DatosPersonalesInvalidos("El nombre no puede superar los 60 caracteres"))
      .when(servicioPerfilMock)
      .actualizarDatosPersonales(eq(usuarioMock), any(), any(), any());

    ModelAndView resultado = controladorPerfil.actualizarDatosPersonales(datos, requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "El nombre no puede superar los 60 caracteres",
      resultado.getModel().get("errorDatos")
    );
  }
}
