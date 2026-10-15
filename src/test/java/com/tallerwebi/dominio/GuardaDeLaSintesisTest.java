package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.consejo.GuardaDeLaSintesis;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** La síntesis de la IA solo se acepta si dice los números del veredicto que calculó Java. */
public class GuardaDeLaSintesisTest {

  // Subieron 9,4 %, inflación 7,6 %, 1,7 % más en términos reales
  private final Veredicto veredicto = new Veredicto(
    YearMonth.of(2026, 5),
    YearMonth.of(2026, 9),
    9.4,
    7.6
  );

  @Test
  void debeAceptarLaSintesisQueDiceLosTresNumerosDelVeredicto() {
    String sintesis =
      "Desde mayo tus gastos subieron 9,4 %, la inflación fue 7,6 % y en términos reales gastás 1,7 % más. Lo que más pesa son los hormiga.";

    assertThat(GuardaDeLaSintesis.respetaElVeredicto(sintesis, veredicto), is(true));
  }

  @Test
  void debeAceptarElNumeroSinElSignoDePorcentaje() {
    String sintesis =
      "Subieron 9,4 por ciento con una inflación de 7,6 por ciento: 1,7 puntos más en términos reales.";

    assertThat(GuardaDeLaSintesis.respetaElVeredicto(sintesis, veredicto), is(true));
  }

  @Test
  void debeRechazarLaSintesisSiFaltaUnNumeroOCambio() {
    String sinInflacion = "Tus gastos subieron 9,4 % y en términos reales gastás 1,7 % más.";
    String otroNumero =
      "Tus gastos subieron 9,5 %, la inflación fue 7,6 % y gastás 1,7 % más en términos reales.";
    String otroNumeroQueContieneElBueno =
      "Tus gastos subieron 19,4 %, la inflación fue 7,6 % y gastás 1,7 % más en términos reales.";

    assertThat(GuardaDeLaSintesis.respetaElVeredicto(sinInflacion, veredicto), is(false));
    assertThat(GuardaDeLaSintesis.respetaElVeredicto(otroNumero, veredicto), is(false));
    assertThat(
      GuardaDeLaSintesis.respetaElVeredicto(otroNumeroQueContieneElBueno, veredicto),
      is(false)
    );
    assertThat(GuardaDeLaSintesis.respetaElVeredicto(null, veredicto), is(false));
  }

  @Test
  void debeAceptarLosEnterosSinDecimalYLasBajas() {
    // Bajaron 2,0 %, inflación 10,0 %, 10,9 % menos en términos reales
    Veredicto baja = new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), -2.0, 10.0);
    String sintesis =
      "Tus gastos bajaron 2 % mientras la inflación fue 10 %: gastás 10,9 % menos en términos reales.";

    assertThat(GuardaDeLaSintesis.respetaElVeredicto(sintesis, baja), is(true));
  }

  @Test
  void debeAceptarLoMismoSinElNumeroCuandoLaDiferenciaRealEsMinima() {
    // 27,0 % contra 26,9 %: 0,1 % real, la regla dice "lo mismo"
    Veredicto casiIgual = new Veredicto(YearMonth.of(2025, 11), YearMonth.of(2026, 9), 27.0, 26.9);
    String sintesis =
      "Subieron 27,0 % y la inflación fue 26,9 %: en términos reales gastás prácticamente lo mismo.";
    String sinNada = "Subieron 27,0 % y la inflación fue 26,9 %, así que vas bien.";

    assertThat(GuardaDeLaSintesis.respetaElVeredicto(sintesis, casiIgual), is(true));
    assertThat(GuardaDeLaSintesis.respetaElVeredicto(sinNada, casiIgual), is(false));
  }
}
