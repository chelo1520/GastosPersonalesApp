package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import com.tallerwebi.dominio.indicadores.InflacionMensual;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

public class InflacionMensualTest {

  @Test
  void debeComponerLaInflacionMesAMesEnVezDeSumarla() {
    List<InflacionMensual> meses = List.of(
      new InflacionMensual(YearMonth.of(2026, 7), 10.0, false),
      new InflacionMensual(YearMonth.of(2026, 8), 10.0, false)
    );

    // 1,10 × 1,10 = 1,21: un 21 %, no un 20 %
    assertThat(InflacionMensual.acumulada(meses), closeTo(21.0, 0.0001));
  }

  @Test
  void debeSerCeroSinMeses() {
    assertThat(InflacionMensual.acumulada(List.of()), closeTo(0.0, 0.0001));
  }
}
