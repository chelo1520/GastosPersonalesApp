package com.tallerwebi.infraestructura.indicadores;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Parseo de respuestas grabadas de las APIs; no hay llamadas de red. */
public class ClienteArgentinaDatosTest {

  private final ClienteArgentinaDatos cliente = new ClienteArgentinaDatos();

  @Test
  void debeParsearLaSerieDelDolarCuandoLaRespuestaTieneVariosDias() {
    String respuesta =
      "[{\"casa\":\"oficial\",\"compra\":1480,\"venta\":1530,\"fecha\":\"2026-09-15\"}," +
      "{\"casa\":\"oficial\",\"compra\":1485,\"venta\":1535,\"fecha\":\"2026-10-13\"}]";

    List<CotizacionDolar> serie = cliente.parsearDolares(respuesta);

    assertThat(serie, hasSize(2));
    assertThat(serie.get(0).getFecha(), equalTo(LocalDate.of(2026, 9, 15)));
    assertThat(serie.get(0).getCompra(), equalTo(1480.0));
    assertThat(serie.get(1).getVenta(), equalTo(1535.0));
  }

  @Test
  void debeTomarLaFechaDeActualizacionComoFechaCuandoParseaElDolarDeHoy() {
    String respuesta =
      "{\"moneda\":\"USD\",\"casa\":\"oficial\",\"nombre\":\"Oficial\",\"compra\":1485," +
      "\"venta\":1535,\"fechaActualizacion\":\"2026-10-13T15:00:00.000Z\"}";

    CotizacionDolar hoy = cliente.parsearDolarDeHoy(respuesta);

    assertThat(hoy.getFecha(), equalTo(LocalDate.of(2026, 10, 13)));
    assertThat(hoy.getVenta(), equalTo(1535.0));
  }

  @Test
  void debeConvertirCadaRegistroEnUnMesNoEstimadoCuandoParseaLaInflacion() {
    String respuesta =
      "[{\"fecha\":\"2026-07-31\",\"valor\":2.1},{\"fecha\":\"2026-08-31\",\"valor\":1.7}]";

    List<InflacionMensual> meses = cliente.parsearInflacion(respuesta);

    assertThat(meses, hasSize(2));
    assertThat(meses.get(1).getMes(), equalTo(YearMonth.of(2026, 8)));
    assertThat(meses.get(1).getPorcentaje(), equalTo(1.7));
    assertThat(meses.get(1).isEstimada(), is(false));
  }

  @Test
  void debeLanzarIndicadoresNoDisponiblesCuandoLaRespuestaNoEsJson() {
    assertThrows(
      IndicadoresNoDisponibles.class,
      () -> cliente.parsearDolares("<html>error</html>")
    );
  }
}
