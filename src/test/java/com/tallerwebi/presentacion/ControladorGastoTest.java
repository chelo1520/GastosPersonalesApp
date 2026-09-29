package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorGastoTest {

  private GastoServicio gastoServicio;
  private ControladorGasto controladorGasto;

  private Usuario usuario;
  private Gasto gasto;

  @BeforeEach
  public void init() {
    gastoServicio = mock(GastoServicio.class);

    controladorGasto = new ControladorGasto(gastoServicio);

    usuario = mock(Usuario.class);

    gasto = new Gasto(15000.0, LocalDate.of(2026, 9, 26), "Supermercado");
  }

  @Test
  public void debeMostrarLosGastosDelUsuario() {
    List<Gasto> gastos = List.of(gasto);

    when(gastoServicio.obtenerGastos(usuario)).thenReturn(gastos);

    ModelAndView resultado = controladorGasto.mostrarGastos(usuario);

    assertEquals("mostrar-gastos", resultado.getViewName());

    assertEquals(gastos, resultado.getModel().get("gastos"));

    verify(gastoServicio).obtenerGastos(usuario);
  }

  @Test
  public void debeRegistrarUnGasto() {
    ModelAndView resultado = controladorGasto.registrarGasto(gasto, usuario);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());

    verify(gastoServicio).registrarGasto(gasto, usuario);
  }

  @Test
  public void debeSumarLosGastosEntreFechas() {
    LocalDate desde = LocalDate.of(2026, 9, 1);
    LocalDate hasta = LocalDate.of(2026, 9, 30);

    when(gastoServicio.sumarGastos(usuario, desde, hasta)).thenReturn(45000.0);

    ModelAndView resultado = controladorGasto.sumarGastos(usuario, desde, hasta);

    assertEquals("sumar-gastos", resultado.getViewName());

    assertEquals(45000.0, resultado.getModel().get("total"));

    verify(gastoServicio).sumarGastos(usuario, desde, hasta);
  }
}
