package com.tallerwebi.infraestructura.asistente;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.RedaccionDeConsejos;
import com.tallerwebi.dominio.asistente.ServicioAsistente;
import com.tallerwebi.dominio.consejo.Consejo;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Redacción de consejos con Claude. Recibe hechos ya calculados y devuelve texto;
 * nunca inventa cifras. Sin ANTHROPIC_API_KEY, con error o pasado el tope diario,
 * devuelve los consejos tal cual (la redacción de las reglas es el fallback).
 */
@Component
public class ServicioAsistenteClaude implements ServicioAsistente {

  static final String VARIABLE_CLAVE = "ANTHROPIC_API_KEY";
  // Modelo por defecto; se puede cambiar con la variable de entorno ASISTENTE_MODELO
  static final String MODELO_POR_DEFECTO = "claude-opus-5-5";
  static final String VARIABLE_MODELO = "ASISTENTE_MODELO";
  // Las consultas se cachean por usuario y mes; esto es solo una red de seguridad de costo
  static final int MAXIMO_LLAMADAS_POR_DIA = 200;
  private static final long MAX_TOKENS = 2000L;
  private static final int MAXIMO_CATEGORIAS_DE_CONTEXTO = 5;

  static final String INSTRUCCIONES =
    "Sos el asistente financiero de Gastitos, una app argentina de gastos personales. " +
    "Recibís en JSON un contexto y una lista de consejos ya calculados por reglas; cada consejo trae " +
    "un id, su severidad y un texto con los hechos (montos, porcentajes, meses). Tu trabajo es " +
    "reescribir cada consejo en español rioplatense, con voseo, en tono cercano y directo, de una o " +
    "dos oraciones, y devolverlos ordenados del más importante al menos importante. Reglas: usá " +
    "solo los números y hechos que recibís, sin inventar ni redondear distinto; no agregues consejos " +
    "nuevos ni omitas ninguno; si un consejo propone un monto, mencionalo tal cual; no uses emojis " +
    "ni signos de exclamación; no repitas el nombre de la app. Respondé únicamente con el JSON pedido.";

  private static final Logger LOG = Logger.getLogger(ServicioAsistenteClaude.class.getName());

  private final boolean conClave;
  private final String modelo;
  private final ObjectMapper json = new ObjectMapper();
  private final Clock reloj;
  private AnthropicClient cliente;
  private LocalDate diaDelContador;
  private int llamadasDelDia;

  public ServicioAsistenteClaude() {
    this(System.getenv(VARIABLE_CLAVE), System.getenv(VARIABLE_MODELO), Clock.systemDefaultZone());
  }

  // Permite construirlo sin clave (tests, CI), elegir el modelo y fijar "hoy"
  public ServicioAsistenteClaude(String claveDeApi, String modelo, Clock reloj) {
    this.reloj = reloj;
    this.modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo;
    this.conClave = claveDeApi != null && !claveDeApi.isBlank();
    if (conClave) {
      this.cliente = AnthropicOkHttpClient.builder().apiKey(claveDeApi).build();
    }
  }

  @Override
  public boolean disponible() {
    return conClave;
  }

  @Override
  public List<Consejo> redactar(List<Consejo> consejos, GastoReal contexto) {
    if (!conClave || !hayAlgoParaRedactar(consejos) || !hayCupoHoy()) {
      return consejos;
    }
    try {
      RedaccionDeConsejos redaccion = consultar(armarHechos(consejos, contexto));
      return redaccion == null ? consejos : aplicarRedaccion(consejos, redaccion);
    } catch (RuntimeException e) {
      // Sin red, clave inválida, respuesta rara: se muestran los consejos de las reglas
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "No se pudo redactar con Claude: {0}", e.getMessage());
      }
      return consejos;
    }
  }

  // Solo se consulta si algún consejo no tiene redacción o sus hechos cambiaron
  private boolean hayAlgoParaRedactar(List<Consejo> consejos) {
    for (Consejo consejo : consejos) {
      if (
        consejo.getTextoRedactado() == null ||
        !consejo.firmaDeLosHechos().equals(consejo.getFirmaHechos())
      ) {
        return true;
      }
    }
    return false;
  }

  // Lo que ve el modelo: hechos agregados, sin email ni gastos individuales
  String armarHechos(List<Consejo> consejos, GastoReal contexto) {
    ObjectNode raiz = json.createObjectNode();
    if (contexto != null) {
      agregarPeriodo(raiz, contexto);
      agregarCategorias(raiz, contexto);
    }
    agregarConsejos(raiz, consejos);
    try {
      return json.writeValueAsString(raiz);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudieron serializar los hechos", e);
    }
  }

  private void agregarPeriodo(ObjectNode raiz, GastoReal contexto) {
    if (contexto.getVeredicto() == null) {
      return;
    }
    ObjectNode periodo = raiz.putObject("periodo");
    periodo.put("desde", contexto.getVeredicto().getDesde().toString());
    periodo.put("hasta", contexto.getVeredicto().getHasta().toString());
    periodo.put(
      "variacionGastosPorcentaje",
      redondear(contexto.getVeredicto().getVariacionNominal())
    );
    periodo.put("inflacionPorcentaje", redondear(contexto.getVeredicto().getInflacionAcumulada()));
    periodo.put("variacionRealPorcentaje", redondear(contexto.getVeredicto().getVariacionReal()));
    periodo.put("estado", contexto.getVeredicto().getEstado().name());
  }

  private void agregarCategorias(ObjectNode raiz, GastoReal contexto) {
    ArrayNode categorias = raiz.putArray("categoriasDelUltimoMesCerrado");
    List<CategoriaGastoReal> todas = contexto.getCategorias();
    for (int i = 0; i < Math.min(todas.size(), MAXIMO_CATEGORIAS_DE_CONTEXTO); i++) {
      CategoriaGastoReal categoria = todas.get(i);
      ObjectNode nodo = categorias.addObject();
      nodo.put("nombre", categoria.getNombre());
      nodo.put("gastado", redondear(categoria.getGastado()));
      if (categoria.getVariacionReal() != null) {
        nodo.put("variacionRealPorcentaje", redondear(categoria.getVariacionReal()));
      }
      nodo.put("diagnostico", categoria.getDiagnostico().getTexto());
    }
  }

  private void agregarConsejos(ObjectNode raiz, List<Consejo> consejos) {
    ArrayNode lista = raiz.putArray("consejos");
    for (Consejo consejo : consejos) {
      ObjectNode nodo = lista.addObject();
      nodo.put("id", consejo.getId());
      nodo.put("tipo", consejo.getTipo().name());
      nodo.put("severidad", consejo.getSeveridad().name());
      if (consejo.getCategoria() != null) {
        nodo.put("categoria", consejo.getCategoria());
      }
      nodo.put("hechos", consejo.getTexto());
      if (consejo.getMontoSugerido() != null) {
        nodo.put("montoSugerido", redondear(consejo.getMontoSugerido()));
      }
    }
  }

  // Aplica la redacción: texto nuevo y orden del modelo; lo que el modelo omitió va al final sin cambios
  List<Consejo> aplicarRedaccion(List<Consejo> consejos, RedaccionDeConsejos redaccion) {
    Map<Long, Consejo> porId = new LinkedHashMap<>();
    for (Consejo consejo : consejos) {
      porId.put(consejo.getId(), consejo);
    }
    List<Consejo> ordenados = new ArrayList<>();
    if (redaccion.consejos() != null) {
      for (RedaccionDeConsejos.ConsejoRedactado redactado : redaccion.consejos()) {
        Consejo consejo = porId.get(redactado.id());
        if (consejo == null || redactado.texto() == null || redactado.texto().isBlank()) {
          // Id desconocido o texto vacío: ese consejo queda con el texto de las reglas
          continue;
        }
        porId.remove(redactado.id());
        consejo.redactar(redactado.texto().trim(), consejo.firmaDeLosHechos());
        ordenados.add(consejo);
      }
    }
    ordenados.addAll(porId.values());
    return ordenados;
  }

  private RedaccionDeConsejos consultar(String hechos) {
    StructuredMessageCreateParams<RedaccionDeConsejos> params = MessageCreateParams
      .builder()
      .model(modelo)
      .maxTokens(MAX_TOKENS)
      .outputConfig(RedaccionDeConsejos.class)
      // Las instrucciones no cambian: se cachean para pagar menos en cada consulta
      .systemOfTextBlockParams(
        List.of(
          TextBlockParam
            .builder()
            .text(INSTRUCCIONES)
            .cacheControl(CacheControlEphemeral.builder().build())
            .build()
        )
      )
      // Si un clasificador de seguridad rechaza el pedido, lo reintenta solo en otro modelo
      .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
      .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
      .addUserMessage(hechos)
      .build();

    contarLlamada();
    var respuesta = cliente.messages().create(params);
    if (
      respuesta.stopReason().isPresent() &&
      "refusal".equals(respuesta.stopReason().get().toString())
    ) {
      return null;
    }
    return respuesta
      .content()
      .stream()
      .flatMap(bloque -> bloque.text().stream())
      .map(texto -> texto.text())
      .findFirst()
      .orElse(null);
  }

  private synchronized boolean hayCupoHoy() {
    LocalDate hoy = LocalDate.now(reloj);
    if (!hoy.equals(diaDelContador)) {
      diaDelContador = hoy;
      llamadasDelDia = 0;
    }
    return llamadasDelDia < MAXIMO_LLAMADAS_POR_DIA;
  }

  private synchronized void contarLlamada() {
    llamadasDelDia++;
  }

  String modeloEnUso() {
    return modelo;
  }

  int llamadasHechasHoy() {
    return llamadasDelDia;
  }

  private static double redondear(double valor) {
    return Math.round(valor * 10) / 10.0;
  }
}
