package com.tallerwebi.infraestructura.asistente;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.RedaccionDeConsejos;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.consejo.Consejo;
import com.tallerwebi.dominio.consejo.TipoConsejo;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Armado de los hechos y aplicación de la respuesta; no hay llamadas a la API. */
public class ServicioAsistenteClaudeTest {

  private ServicioAsistenteClaude sinClave;
  private Usuario usuario;
  private Consejo presupuesto;
  private Consejo tendencia;

  @BeforeEach
  public void init() {
    sinClave = new ServicioAsistenteClaude(null, null, Clock.systemDefaultZone());
    usuario = new Usuario();
    usuario.setEmail("diego@unlam.edu.ar");
    presupuesto =
      consejo(
        1L,
        TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
        "Alimentación",
        "Tu presupuesto quedó atrás",
        220000.0
      );
    tendencia = consejo(2L, TipoConsejo.VEREDICTO_AMARILLO, null, "Gastás más en serio", null);
  }

  @Test
  void debeDevolverLosConsejosTalCualYNoEstarDisponibleCuandoNoHayClave() {
    List<Consejo> consejos = List.of(presupuesto, tendencia);

    List<Consejo> resultado = sinClave.redactar(consejos, null);

    assertThat(sinClave.disponible(), is(false));
    assertThat(resultado, sameInstance(consejos));
    assertThat(presupuesto.getTextoRedactado(), nullValue());
  }

  @Test
  void debeMandarLosHechosDeCadaConsejoYElContextoSinDatosPersonales() {
    GastoReal contexto = new GastoReal(
      List.of(),
      new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 9.4, 7.6),
      YearMonth.of(2026, 9),
      List.of(new CategoriaGastoReal("Servicios", 70200.0, 89000.0, 1.7)),
      null,
      true
    );

    String hechos = sinClave.armarHechos(List.of(presupuesto, tendencia), contexto);

    assertThat(hechos, containsString("\"id\":1"));
    assertThat(hechos, containsString("\"montoSugerido\":220000.0"));
    assertThat(hechos, containsString("\"familia\":\"PROPUESTA\""));
    assertThat(hechos, containsString("\"familia\":\"LECTURA\""));
    assertThat(hechos, containsString("\"hechos\":\"Tu presupuesto quedó atrás\""));
    assertThat(hechos, containsString("\"variacionGastosPorcentaje\":9.4"));
    assertThat(hechos, containsString("\"nombre\":\"Servicios\""));
    assertThat(hechos, containsString("\"diagnostico\":\"Ahorraste\""));
    assertThat(hechos, not(containsString("diego@unlam.edu.ar")));
  }

  @Test
  void debeAplicarElTextoYElOrdenDelModeloYDejarAlFinalLoQueOmitio() {
    Consejo extra = consejo(
      3L,
      TipoConsejo.CATEGORIA_SUBE_MAS_QUE_LA_INFLACION,
      "Cine",
      "Cine sube",
      null
    );
    RedaccionDeConsejos redaccion = new RedaccionDeConsejos(
      List.of(
        new RedaccionDeConsejos.ConsejoRedactado(2L, "Ojo: estás gastando más en serio."),
        new RedaccionDeConsejos.ConsejoRedactado(1L, " Subí Alimentación a $ 220.000. "),
        new RedaccionDeConsejos.ConsejoRedactado(99L, "Un consejo inventado")
      )
    );

    List<Consejo> resultado = sinClave.aplicarRedaccion(
      List.of(presupuesto, tendencia, extra),
      redaccion
    );

    assertThat(resultado, hasSize(3));
    assertThat(resultado.get(0), sameInstance(tendencia));
    assertThat(
      resultado.get(0).getTextoParaMostrar(),
      equalTo("Ojo: estás gastando más en serio.")
    );
    assertThat(resultado.get(1).getTextoParaMostrar(), equalTo("Subí Alimentación a $ 220.000."));
    assertThat(resultado.get(1).getFirmaHechos(), equalTo(presupuesto.firmaDeLosHechos()));
    // El omitido conserva el texto de las reglas
    assertThat(resultado.get(2), sameInstance(extra));
    assertThat(resultado.get(2).getTextoParaMostrar(), equalTo("Cine sube"));
  }

  @Test
  void debeIgnorarLasRedaccionesVaciasYConservarElTextoDeLasReglas() {
    RedaccionDeConsejos redaccion = new RedaccionDeConsejos(
      List.of(new RedaccionDeConsejos.ConsejoRedactado(1L, "   "))
    );

    List<Consejo> resultado = sinClave.aplicarRedaccion(List.of(presupuesto), redaccion);

    assertThat(resultado.get(0).getTextoParaMostrar(), equalTo("Tu presupuesto quedó atrás"));
  }

  @Test
  void debeVolverARedactarSoloCuandoCambianLosHechosDelConsejo() {
    presupuesto.redactar("Redactado", presupuesto.firmaDeLosHechos());
    String firmaAnterior = presupuesto.getFirmaHechos();

    presupuesto.setMontoSugerido(230000.0);

    assertThat(presupuesto.firmaDeLosHechos(), not(equalTo(firmaAnterior)));
  }

  @Test
  void debeUsarHaiku55SalvoQueSeIndiqueOtroModelo() {
    assertThat(sinClave.modeloEnUso(), equalTo("claude-haiku-5-5"));
    assertThat(
      new ServicioAsistenteClaude(null, "claude-opus-5-5", Clock.systemDefaultZone()).modeloEnUso(),
      equalTo("claude-opus-5-5")
    );
  }

  private Consejo consejo(Long id, TipoConsejo tipo, String categoria, String texto, Double monto) {
    Consejo consejo = new Consejo(usuario, YearMonth.of(2026, 10), tipo, categoria, texto, monto);
    consejo.setId(id);
    return consejo;
  }
}
