package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.resumen.CalculadoraGastosHormiga;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenHormiga;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CalculadoraGastosHormigaTest {

  private static final double PRESUPUESTO = 550000.0;

  private CalculadoraGastosHormiga calculadora;
  private Categoria cafe;
  private Categoria kiosco;
  private Categoria delivery;
  private Categoria supermercado;

  @BeforeEach
  public void init() {
    calculadora = new CalculadoraGastosHormiga();
    cafe = categoria("Café y snacks", false);
    kiosco = categoria("Kiosco", false);
    delivery = categoria("Delivery", false);
    supermercado = categoria("Supermercado", true);
  }

  @Test
  void debeSumar44400QueEsEl8Coma1PorCientoConElEjemploDelDoc() {
    List<Gasto> gastos = new ArrayList<>();
    gastos.addAll(gastos(12, 2500.0, cafe));
    gastos.addAll(gastos(8, 1800.0, kiosco));
    gastos.addAll(gastos(3, 4000.0, delivery));
    gastos.addAll(gastos(8, 1500.0, supermercado));

    ResumenHormiga hormiga = calculadora.calcular(gastos, PRESUPUESTO);

    assertEquals(44400.0, hormiga.getTotal());
    assertEquals(8.07, hormiga.getPorcentajePresupuesto(), 0.01);
    assertEquals(
      List.of("Café y snacks", "Kiosco"),
      hormiga.getCategorias().stream().map(ResumenCategoria::getNombre).toList()
    );
    assertEquals(30000.0, hormiga.getCategorias().get(0).getGastado());
    assertEquals(14400.0, hormiga.getCategorias().get(1).getGastado());
  }

  @Test
  void noDebeContarUnaCategoriaEsencialAunqueTengaMuchosGastosChicos() {
    ResumenHormiga hormiga = calculadora.calcular(gastos(10, 1500.0, supermercado), PRESUPUESTO);

    assertEquals(0.0, hormiga.getTotal());
    assertTrue(hormiga.getCategorias().isEmpty());
  }

  @Test
  void noDebeContarUnaCategoriaConMenosDe4GastosChicos() {
    ResumenHormiga hormiga = calculadora.calcular(gastos(3, 4000.0, delivery), PRESUPUESTO);

    assertEquals(0.0, hormiga.getTotal());
  }

  @Test
  void noDebeContarLosGastosDeMasDel2PorCientoDelPresupuestoNiParaLaFrecuencia() {
    // El 2 % de 550.000 es 11.000: el de 11.000 cuenta, los de 11.001 no
    List<Gasto> gastos = new ArrayList<>();
    gastos.addAll(gastos(3, 11001.0, delivery));
    gastos.addAll(gastos(3, 11000.0, delivery));

    ResumenHormiga hormiga = calculadora.calcular(gastos, PRESUPUESTO);

    assertEquals(0.0, hormiga.getTotal());

    gastos.addAll(gastos(1, 11000.0, delivery));
    hormiga = calculadora.calcular(gastos, PRESUPUESTO);

    assertEquals(44000.0, hormiga.getTotal());
  }

  @Test
  void debeTratarComoEsencialesLosGastosSinCategoriaYLasCategoriasSinMarcar() {
    List<Gasto> gastos = new ArrayList<>();
    gastos.addAll(gastos(5, 1000.0, null));
    gastos.addAll(gastos(5, 1000.0, categoria("Otros", null)));

    ResumenHormiga hormiga = calculadora.calcular(gastos, PRESUPUESTO);

    assertEquals(0.0, hormiga.getTotal());
  }

  private List<Gasto> gastos(int cantidad, double importe, Categoria categoria) {
    List<Gasto> gastos = new ArrayList<>();
    for (int i = 0; i < cantidad; i++) {
      Gasto gasto = new Gasto(importe, LocalDate.of(2026, 10, 1 + (i % 20)), "Gasto " + i);
      gasto.setCategoria(categoria);
      gastos.add(gasto);
    }
    return gastos;
  }

  private Categoria categoria(String nombre, Boolean esencial) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    categoria.setEsencial(esencial);
    return categoria;
  }
}
