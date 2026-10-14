package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.alerta.CalculadoraAlertas;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.resumen.ResumenHormiga;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CalculadoraAlertasTest {

  private static final double PRESUPUESTO = 550000.0;

  private CalculadoraAlertas calculadora;
  private Categoria cafe;

  @BeforeEach
  public void init() {
    calculadora = new CalculadoraAlertas();
    cafe = new Categoria();
    cafe.setId(7L);
    cafe.setNombre("Café y snacks");
    cafe.setEsencial(false);
  }

  @Test
  void debeDarSoloElAcumuladoAmarilloConElEjemploDelDoc() {
    ResumenHormiga hormiga = hormiga(44400.0, 8.07);

    List<Alerta> alertas = calculadora.alertasDelMes(hormiga);

    assertEquals(1, alertas.size());
    assertEquals(Alerta.Tipo.ACUMULADO, alertas.get(0).getTipo());
    assertEquals(Alerta.Nivel.AMARILLA, alertas.get(0).getNivel());
    assertEquals(
      "Llevás $44.400 en gastos hormiga, el 8% de tu presupuesto",
      alertas.get(0).getMensaje()
    );
  }

  @Test
  void debeDarAcumuladoRojaCuandoYaSeLlegoAl10PorCiento() {
    List<Alerta> alertas = calculadora.alertasDelMes(hormiga(60500.0, 11.0));

    assertEquals(1, alertas.size());
    assertEquals(Alerta.Tipo.ACUMULADO, alertas.get(0).getTipo());
    assertEquals(Alerta.Nivel.ROJA, alertas.get(0).getNivel());
  }

  @Test
  void noDebeDarAlertasCuandoLosGastosHormigaSonPocos() {
    assertTrue(calculadora.alertasDelMes(hormiga(11000.0, 2.0)).isEmpty());
  }

  @Test
  void debeAlertarAlRegistrarElCuartoGastoChicoDeLaCategoriaEnLaSemana() {
    List<Gasto> semana = cafes(4);

    Alerta alerta = calculadora.alertaDeRepeticion(semana.get(3), semana, PRESUPUESTO);

    assertEquals(Alerta.Tipo.REPETICION, alerta.getTipo());
    assertEquals("Es tu 4.º gasto en Café y snacks esta semana ($10.000)", alerta.getMensaje());
    assertEquals(7L, alerta.getCategoriaId());
  }

  @Test
  void noDebeAlertarConElTercerGastoChico() {
    List<Gasto> semana = cafes(3);

    assertNull(calculadora.alertaDeRepeticion(semana.get(2), semana, PRESUPUESTO));
  }

  @Test
  void noDebeAlertarCuandoElGastoNuevoEsDeUnaCategoriaEsencial() {
    List<Gasto> semana = cafes(4);
    cafe.setEsencial(true);

    assertNull(calculadora.alertaDeRepeticion(semana.get(3), semana, PRESUPUESTO));
  }

  @Test
  void noDebeContarLosGastosDeOtraCategoria() {
    Categoria kiosco = new Categoria();
    kiosco.setId(8L);
    kiosco.setNombre("Kiosco");
    kiosco.setEsencial(false);
    List<Gasto> semana = cafes(3);
    Gasto golosina = new Gasto(1800.0, LocalDate.of(2026, 10, 10), "Golosina");
    golosina.setCategoria(kiosco);
    semana.add(golosina);

    assertNull(calculadora.alertaDeRepeticion(semana.get(2), semana, PRESUPUESTO));
  }

  @Test
  void debeAvisarEnAmarilloConElGastoQueHaceLlegarAl80PorCientoDelPresupuestoDelMes() {
    // Antes 400.000 (72 %), con el gasto de 40.000 llega a 440.000 (80 %)
    Alerta alerta = calculadora.alertaDePresupuesto(440000.0, 40000.0, PRESUPUESTO);

    assertEquals(Alerta.Tipo.PRESUPUESTO, alerta.getTipo());
    assertEquals(Alerta.Nivel.AMARILLA, alerta.getNivel());
    assertEquals(
      "Con este gasto llegaste al 80% del presupuesto del mes ($440.000 de $550.000)",
      alerta.getMensaje()
    );
  }

  @Test
  void debeAvisarEnRojoConElGastoQueHacePasarElPresupuestoDelMes() {
    Alerta alerta = calculadora.alertaDePresupuesto(560000.0, 30000.0, PRESUPUESTO);

    assertEquals(Alerta.Nivel.ROJA, alerta.getNivel());
    assertEquals(
      "Con este gasto te pasaste del presupuesto del mes: llevás $560.000 de $550.000",
      alerta.getMensaje()
    );
  }

  @Test
  void debeAvisarEnRojoCuandoUnSoloGastoPasaDeMenosDel80AMasDel100() {
    Alerta alerta = calculadora.alertaDePresupuesto(600000.0, 300000.0, PRESUPUESTO);

    assertEquals(Alerta.Nivel.ROJA, alerta.getNivel());
  }

  @Test
  void noDebeVolverAAvisarConLosGastosSiguientesDentroDelMismoTramo() {
    // Ya estaba en 85 %, sigue en amarillo
    assertNull(calculadora.alertaDePresupuesto(480000.0, 12500.0, PRESUPUESTO));
    // Ya estaba pasado
    assertNull(calculadora.alertaDePresupuesto(620000.0, 20000.0, PRESUPUESTO));
  }

  @Test
  void noDebeAvisarCuandoElMesSigueDebajoDel80PorCiento() {
    assertNull(calculadora.alertaDePresupuesto(300000.0, 50000.0, PRESUPUESTO));
  }

  @Test
  void noDebeAvisarCuandoNoHayPresupuestoDefinido() {
    assertNull(calculadora.alertaDePresupuesto(300000.0, 50000.0, 0.0));
  }

  @Test
  void noDebeDecir100PorCientoSiTodaviaNoSePaso() {
    Alerta alerta = calculadora.alertaDePresupuesto(549000.0, 200000.0, PRESUPUESTO);

    assertEquals(Alerta.Nivel.AMARILLA, alerta.getNivel());
    assertTrue(alerta.getMensaje().startsWith("Con este gasto llegaste al 99%"));
  }

  private ResumenHormiga hormiga(double total, double porcentaje) {
    return new ResumenHormiga(total, porcentaje, List.of());
  }

  private List<Gasto> cafes(int cantidad) {
    List<Gasto> gastos = new ArrayList<>();
    for (int i = 0; i < cantidad; i++) {
      Gasto gasto = new Gasto(2500.0, LocalDate.of(2026, 10, 10 + i), "Café");
      gasto.setCategoria(cafe);
      gastos.add(gasto);
    }
    return gastos;
  }
}
