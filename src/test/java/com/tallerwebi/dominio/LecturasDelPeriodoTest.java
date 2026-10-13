package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.MesGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.consejo.Consejo;
import com.tallerwebi.dominio.consejo.LecturasDelPeriodo;
import com.tallerwebi.dominio.consejo.TipoConsejo;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Las lecturas del período (HU-16): conclusiones calculadas en Java sobre el gasto real. */
public class LecturasDelPeriodoTest {

  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);
  private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
  private static final double IPC = 1.7;

  private LecturasDelPeriodo lecturas;
  private Usuario usuario;

  @BeforeEach
  public void init() {
    lecturas = new LecturasDelPeriodo();
    usuario = new Usuario();
    usuario.setId(1L);
  }

  @Test
  void debeDarElVeredictoComoPrimeraLectura() {
    GastoReal gastoReal = gastoReal(
      List.of(),
      List.of(),
      new Veredicto(YearMonth.of(2026, 5), SEPTIEMBRE, 30.0, 10.0)
    );

    List<Consejo> resultado = lecturas.calcular(usuario, OCTUBRE, gastoReal);

    assertThat(resultado, hasSize(1));
    assertThat(resultado.get(0).getTipo(), equalTo(TipoConsejo.VEREDICTO_ROJO));
    assertThat(resultado.get(0).getTexto(), containsString("gastás 18,2 % más en serio."));
  }

  @Test
  void debeDecirQueElMesCerradoFueMejorYContarLaRachaCuandoBajaTresMesesSeguidos() {
    // En pesos de hoy: 820.000 → 806.575 → 790.300 → 773.937, tres bajas seguidas
    List<MesGastoReal> meses = List.of(
      mes(YearMonth.of(2026, 6), 820000.0, 820000.0, 0.0, 0.0, false),
      mes(YearMonth.of(2026, 7), 766800.0, 806575.0, 0.0, 0.0, false),
      mes(YearMonth.of(2026, 8), 764100.0, 790300.0, 0.0, 0.0, false),
      mes(SEPTIEMBRE, 761000.0, 773937.0, 0.0, 0.0, false),
      mes(OCTUBRE, 300000.0, 300000.0, 0.0, 0.0, true)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(meses, List.of(), null)
    );

    assertThat(resultado, hasSize(1));
    Consejo balance = resultado.get(0);
    assertThat(balance.getTipo(), equalTo(TipoConsejo.BALANCE_DEL_MES_MEJOR));
    assertThat(balance.isLectura(), is(true));
    assertThat(
      balance.getTexto(),
      containsString(
        "Septiembre 2026 costó $ 773.937 en pesos de hoy, 2,1 % menos que Agosto 2026."
      )
    );
    assertThat(balance.getTexto(), containsString("Es el 3.º mes seguido bajando en serio."));
  }

  @Test
  void debeDecirQueElMesFuePeorSinRachaCuandoSubioUnaSolaVez() {
    List<MesGastoReal> meses = List.of(
      mes(YearMonth.of(2026, 7), 800000.0, 800000.0, 0.0, 0.0, false),
      mes(YearMonth.of(2026, 8), 760000.0, 770000.0, 0.0, 0.0, false),
      mes(SEPTIEMBRE, 800000.0, 800000.0, 0.0, 0.0, false)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(meses, List.of(), null)
    );

    assertThat(resultado.get(0).getTipo(), equalTo(TipoConsejo.BALANCE_DEL_MES_PEOR));
    assertThat(resultado.get(0).getTexto(), containsString("3,9 % más que Agosto 2026."));
    assertThat(resultado.get(0).getTexto(), not(containsString("mes seguido")));
  }

  @Test
  void noDebeDarBalanceCuandoLaDiferenciaRealEsMenorAUnPorciento() {
    List<MesGastoReal> meses = List.of(
      mes(YearMonth.of(2026, 8), 760000.0, 770000.0, 0.0, 0.0, false),
      mes(SEPTIEMBRE, 765000.0, 765000.0, 0.0, 0.0, false)
    );

    assertThat(lecturas.calcular(usuario, OCTUBRE, gastoReal(meses, List.of(), null)), hasSize(0));
  }

  @Test
  void debeSenialarLaMayorBajaRealSoloEntreLasCategoriasQuePesan() {
    // Educación cayó más, pero pesa menos del 5 % del mes; Servicios es la que cuenta
    List<CategoriaGastoReal> categorias = List.of(
      new CategoriaGastoReal("Vivienda", 331700.0, 325000.0, IPC),
      new CategoriaGastoReal("Alimentación", 200000.0, 195000.0, IPC),
      new CategoriaGastoReal("Servicios", 70200.0, 89000.0, IPC),
      new CategoriaGastoReal("Educación", 11700.0, 20000.0, IPC)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(List.of(), categorias, null)
    );

    Consejo baja = soloDeTipo(resultado, TipoConsejo.MAYOR_BAJA_REAL);
    assertThat(baja.getCategoria(), equalTo("Servicios"));
    assertThat(
      baja.getTexto(),
      containsString(
        "Servicios fue lo que más bajó en serio en Septiembre 2026: 22,4 % menos que en Agosto 2026"
      )
    );
    assertThat(baja.getTexto(), containsString("($ 70.200 contra $ 89.000)"));
  }

  @Test
  void debeSenialarLaMayorSubaRealConAtencion() {
    List<CategoriaGastoReal> categorias = List.of(
      new CategoriaGastoReal("Alimentación", 200000.0, 195000.0, IPC),
      new CategoriaGastoReal("Entretenimiento", 80000.0, 60000.0, IPC)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(List.of(), categorias, null)
    );

    Consejo suba = soloDeTipo(resultado, TipoConsejo.MAYOR_SUBA_REAL);
    assertThat(suba.getCategoria(), equalTo("Entretenimiento"));
    assertThat(suba.getTexto(), containsString("Entretenimiento fue lo que más subió en serio"));
    assertThat(suba.getTexto(), containsString("31,1 % más que en Agosto 2026"));
  }

  @Test
  void debeLeerLosHabitosCuandoLaMayoriaDeLasCategoriasSeMovioConLaInflacion() {
    List<CategoriaGastoReal> categorias = List.of(
      new CategoriaGastoReal("Vivienda", 331700.0, 325000.0, IPC),
      new CategoriaGastoReal("Alimentación", 200000.0, 195000.0, IPC),
      new CategoriaGastoReal("Transporte", 46800.0, 45700.0, IPC),
      new CategoriaGastoReal("Servicios", 70200.0, 89000.0, IPC),
      new CategoriaGastoReal("Cine", 5000.0, 0.0, IPC)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(List.of(), categorias, null)
    );

    Consejo habitos = soloDeTipo(resultado, TipoConsejo.HABITOS_CON_LA_INFLACION);
    // Cine es nueva y no compara; de las 4 comparables, 3 solo subieron de precio
    assertThat(
      habitos.getTexto(),
      containsString(
        "En 3 de 4 categorías gastaste en Septiembre 2026 lo mismo en serio que en Agosto 2026"
      )
    );
  }

  @Test
  void noDebeLeerHabitosConMenosDeTresCategoriasComparables() {
    List<CategoriaGastoReal> categorias = List.of(
      new CategoriaGastoReal("Vivienda", 331700.0, 325000.0, IPC),
      new CategoriaGastoReal("Alimentación", 200000.0, 195000.0, IPC)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(List.of(), categorias, null)
    );

    assertThat(deTipo(resultado, TipoConsejo.HABITOS_CON_LA_INFLACION), hasSize(0));
  }

  @Test
  void debeAvisarHormigaAltaConElMargenElPicoLaEquivalenciaYLoQueVaDelMes() {
    // Seis meses cerrados con presupuesto 840.000 y gastado 760.000: sobran 80.000 por mes
    List<MesGastoReal> meses = new ArrayList<>();
    double[] hormiga = { 54300, 55700, 57100, 70200, 60000, 61500 };
    for (int i = 0; i < hormiga.length; i++) {
      YearMonth mes = YearMonth.of(2026, 4 + i);
      meses.add(mes(mes, 760000.0, 760000.0, hormiga[i], 840000.0, false));
    }
    meses.add(mes(OCTUBRE, 300000.0, 300000.0, 12000.0, 840000.0, true));
    List<CategoriaGastoReal> categorias = List.of(
      new CategoriaGastoReal("Vivienda", 331700.0, 325000.0, IPC),
      new CategoriaGastoReal("Alimentación", 200000.0, 195000.0, IPC)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(meses, categorias, null)
    );

    Consejo lectura = soloDeTipo(resultado, TipoConsejo.HORMIGA_ALTA);
    String texto = lectura.getTexto();
    assertThat(
      texto,
      containsString(
        "Entre Abril 2026 y Septiembre 2026 se te fueron $ 358.800 en gastos hormiga, el 7,1 % del presupuesto"
      )
    );
    assertThat(
      texto,
      containsString("y el 75 % de lo que te sobró; el pico fue Julio 2026 ($ 70.200).")
    );
    // Vivienda es la más grande pero no cuenta: la equivalencia va contra Alimentación
    assertThat(texto, containsString("Con la mitad pagabas 0,9 meses de Alimentación."));
    assertThat(texto, containsString("Este mes ya llevás $ 12.000."));
  }

  @Test
  void debeDecirQueLosHormigaEstanBajoControlCuandoSonMenosDelDosPorciento() {
    List<MesGastoReal> meses = List.of(
      mes(YearMonth.of(2026, 8), 760000.0, 770000.0, 10000.0, 840000.0, false),
      mes(SEPTIEMBRE, 765000.0, 765000.0, 10000.0, 840000.0, false)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(meses, List.of(), null)
    );

    Consejo lectura = soloDeTipo(resultado, TipoConsejo.HORMIGA_CONTROLADA);
    assertThat(
      lectura.getTexto(),
      containsString("Tus gastos hormiga están bajo control: $ 20.000")
    );
    assertThat(lectura.getTexto(), containsString("el 1,2 % del presupuesto."));
  }

  @Test
  void debeDecirQueNoHuboHormigaCuandoHayPresupuestoPeroNingunGastoHormiga() {
    List<MesGastoReal> meses = List.of(
      mes(YearMonth.of(2026, 8), 760000.0, 770000.0, 0.0, 840000.0, false),
      mes(SEPTIEMBRE, 765000.0, 765000.0, 0.0, 840000.0, false)
    );

    List<Consejo> resultado = lecturas.calcular(
      usuario,
      OCTUBRE,
      gastoReal(meses, List.of(), null)
    );

    Consejo lectura = soloDeTipo(resultado, TipoConsejo.HORMIGA_CONTROLADA);
    assertThat(
      lectura.getTexto(),
      equalTo(
        "No tuviste gastos hormiga entre Agosto 2026 y Septiembre 2026: lo chico y prescindible está bajo control."
      )
    );
  }

  @Test
  void noDebeHablarDeHormigaSinPresupuestoNiConUnSoloMesCerrado() {
    List<MesGastoReal> sinPresupuesto = List.of(
      mes(YearMonth.of(2026, 8), 760000.0, 770000.0, 60000.0, 0.0, false),
      mes(SEPTIEMBRE, 765000.0, 765000.0, 60000.0, 0.0, false)
    );
    List<MesGastoReal> unSoloMes = List.of(
      mes(SEPTIEMBRE, 765000.0, 765000.0, 60000.0, 840000.0, false),
      mes(OCTUBRE, 300000.0, 300000.0, 12000.0, 840000.0, true)
    );

    assertThat(
      deTipo(
        lecturas.calcular(usuario, OCTUBRE, gastoReal(sinPresupuesto, List.of(), null)),
        TipoConsejo.HORMIGA_ALTA
      ),
      hasSize(0)
    );
    assertThat(
      deTipo(
        lecturas.calcular(usuario, OCTUBRE, gastoReal(unSoloMes, List.of(), null)),
        TipoConsejo.HORMIGA_ALTA
      ),
      hasSize(0)
    );
  }

  private Consejo soloDeTipo(List<Consejo> consejos, TipoConsejo tipo) {
    List<Consejo> encontrados = deTipo(consejos, tipo);
    assertThat(encontrados, hasSize(1));
    return encontrados.get(0);
  }

  private List<Consejo> deTipo(List<Consejo> consejos, TipoConsejo tipo) {
    return consejos.stream().filter(consejo -> consejo.getTipo() == tipo).toList();
  }

  private MesGastoReal mes(
    YearMonth mes,
    Double gastado,
    Double enPesosDeHoy,
    Double hormiga,
    Double presupuesto,
    boolean enCurso
  ) {
    return new MesGastoReal(
      mes,
      gastado,
      enPesosDeHoy,
      IPC,
      false,
      1500.0,
      enCurso,
      hormiga,
      presupuesto
    );
  }

  private GastoReal gastoReal(
    List<MesGastoReal> meses,
    List<CategoriaGastoReal> categorias,
    Veredicto veredicto
  ) {
    return new GastoReal(meses, veredicto, SEPTIEMBRE, categorias, null, true);
  }
}
