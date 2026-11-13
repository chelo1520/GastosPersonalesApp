package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class MesDeGastosTest {

  private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);
  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

  private Categoria cafe;
  private Categoria alimentacion;
  private long proximoId;

  @BeforeEach
  public void init() {
    cafe = categoria("Café y snacks");
    alimentacion = categoria("Alimentación");
    proximoId = 1;
  }

  @Test
  public void debeTenerUnaCasillaPorDiaDelMesConLosDiasDespuesDeHoyComoFuturos() {
    MesDeGastos mes = new MesDeGastos(List.of(), NOVIEMBRE, HOY);

    List<MesDeGastos.Dia> dias = mes.getDias();

    assertThat(dias, hasSize(30));
    assertThat(dias.get(11).isFuturo(), is(false));
    assertThat(dias.get(12).isFuturo(), is(true));
    assertThat(dias.get(0).getNivel(), is(0));
  }

  @Test
  public void losNivelesVanPorPuestoAunqueUnDiaTengaUnGastoEnorme() {
    List<Gasto> gastos = List.of(
      gasto(500000.0, 1, alimentacion, "Alquiler"),
      gasto(10000.0, 5, cafe, "Cafés"),
      gasto(4000.0, 6, cafe, "Medialunas"),
      gasto(500.0, 7, cafe, "Caramelos")
    );

    List<MesDeGastos.Dia> dias = new MesDeGastos(gastos, NOVIEMBRE, HOY).getDias();

    assertThat(dias.get(0).getNivel(), is(4));
    assertThat(dias.get(4).getNivel(), is(3));
    assertThat(dias.get(5).getNivel(), is(2));
    assertThat(dias.get(6).getNivel(), is(1));
    assertThat(dias.get(1).getNivel(), is(0));
    assertThat(dias.get(4).getDescripcion(), equalTo("5/11 · $ 10.000,00"));
  }

  @Test
  public void conUnSoloDiaConGastoEseDiaTieneElNivelMaximo() {
    assertThat(MesDeGastos.nivel(0, List.of()), is(0));
    assertThat(MesDeGastos.nivel(800, List.of(800.0)), is(4));
    assertThat(MesDeGastos.nivel(800, List.of(800.0, 900.0)), is(1));
    assertThat(MesDeGastos.nivel(900, List.of(800.0, 900.0)), is(4));
  }

  @Test
  public void debeAgruparPorDiaDelMasRecienteConHoyAyerYElDiaDeLaSemana() {
    List<Gasto> gastos = List.of(
      gasto(1000.0, 10, cafe, "Cortado"),
      gasto(2500.0, 12, cafe, "Café"),
      gasto(46200.0, 12, alimentacion, "Compra semanal"),
      gasto(3000.0, 11, cafe, "Medialunas")
    );

    List<MesDeGastos.Grupo> grupos = new MesDeGastos(gastos, NOVIEMBRE, HOY).getGrupos();

    assertThat(
      grupos.stream().map(MesDeGastos.Grupo::getTitulo).toList(),
      contains("Hoy · 12 de noviembre", "Ayer · 11 de noviembre", "Martes 10 de noviembre")
    );
    assertThat(grupos.get(0).getTotal(), is(48700.0));
    assertThat(grupos.get(0).getDia(), is(12));
    // Dentro del día, el último cargado primero
    assertThat(
      grupos.get(0).getFilas().stream().map(MesDeGastos.Fila::getDescripcion).toList(),
      contains("Compra semanal", "Café")
    );
  }

  @Test
  public void debeIgnorarLosGastosDeOtroMes() {
    Gasto deOctubre = new Gasto(9000.0, LocalDate.of(2026, 10, 31), "Octubre");
    deOctubre.setCategoria(cafe);
    List<Gasto> gastos = new ArrayList<>(List.of(gasto(1000.0, 1, cafe, "Noviembre")));
    gastos.add(deOctubre);

    MesDeGastos mes = new MesDeGastos(gastos, NOVIEMBRE, HOY);

    assertThat(mes.getCantidad(), is(1));
    assertThat(mes.getTotal(), is(1000.0));
  }

  @Test
  public void debeOrdenarPorImporteConLaFechaCortaEnCadaFila() {
    List<Gasto> gastos = List.of(
      gasto(1000.0, 10, cafe, "Cortado"),
      gasto(46200.0, 3, alimentacion, "Compra semanal"),
      gasto(2500.0, 12, cafe, "Café")
    );

    List<MesDeGastos.Fila> filas = new MesDeGastos(gastos, NOVIEMBRE, HOY).getPorImporte();

    assertThat(
      filas.stream().map(MesDeGastos.Fila::getDescripcion).toList(),
      contains("Compra semanal", "Café", "Cortado")
    );
    assertThat(filas.get(0).getFechaCorta(), equalTo("3 nov"));
    assertThat(filas.get(0).getInicial(), equalTo("C"));
    assertThat(filas.get(0).getColor(), equalTo("#7fc4b8"));
    assertThat(filas.get(0).getDia(), is(3));
  }

  @Test
  public void debeSumarPorCategoriaConLaBarraRelativaALaDeMasGasto() {
    List<Gasto> gastos = List.of(
      gasto(2500.0, 12, cafe, "Café"),
      gasto(2500.0, 11, cafe, "Café"),
      gasto(20000.0, 3, alimentacion, "Compra")
    );

    List<MesDeGastos.TotalDeCategoria> categorias = new MesDeGastos(gastos, NOVIEMBRE, HOY)
      .getCategorias();

    assertThat(
      categorias.stream().map(MesDeGastos.TotalDeCategoria::getNombre).toList(),
      contains("Alimentación", "Café y snacks")
    );
    assertThat(categorias.get(0).getAncho(), is(100L));
    assertThat(categorias.get(1).getAncho(), is(25L));
    assertThat(categorias.get(1).getTotal(), is(5000.0));
  }

  @Test
  public void unGastoSinCategoriaSeMuestraComoSinCategoriaYSinDescripcionConSigno() {
    Gasto sinNada = new Gasto(100.0, LocalDate.of(2026, 11, 2), " ");
    ReflectionTestUtils.setField(sinNada, "id", 99L);

    MesDeGastos mes = new MesDeGastos(List.of(sinNada), NOVIEMBRE, HOY);

    MesDeGastos.Fila fila = mes.getGrupos().get(0).getFilas().get(0);
    assertThat(fila.getCategoria(), equalTo("Sin categoría"));
    assertThat(fila.getInicial(), equalTo("?"));
    assertThat(mes.getCategorias().get(0).getColor(), equalTo("#8a8580"));
    assertThat(mes.isVacio(), is(false));
  }

  private Gasto gasto(Double importe, int dia, Categoria categoria, String descripcion) {
    Gasto gasto = new Gasto(importe, NOVIEMBRE.atDay(dia), descripcion);
    gasto.setCategoria(categoria);
    ReflectionTestUtils.setField(gasto, "id", proximoId++);
    return gasto;
  }

  private Categoria categoria(String nombre) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    return categoria;
  }
}
