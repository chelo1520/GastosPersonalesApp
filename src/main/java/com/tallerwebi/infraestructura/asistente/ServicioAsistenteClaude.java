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
import com.tallerwebi.dominio.asistente.ServicioAsistente;
import com.tallerwebi.dominio.asistente.SintesisDelPeriodo;
import com.tallerwebi.dominio.consejo.Consejo;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Síntesis del período con Claude: recibe las conclusiones ya calculadas por las reglas y devuelve
 * un solo párrafo que las conecta; nunca inventa cifras. Sin ANTHROPIC_API_KEY, con error o pasado
 * el tope diario, devuelve null y se muestra el veredicto de las reglas.
 */
@Component
public class ServicioAsistenteClaude implements ServicioAsistente {

  static final String VARIABLE_CLAVE = "ANTHROPIC_API_KEY";
  // Modelo por defecto: la tarea es un párrafo con números dados, Haiku alcanza y es
  // 40 veces más barato que Opus. Se cambia con la variable de entorno ASISTENTE_MODELO
  static final String MODELO_POR_DEFECTO = "claude-haiku-5-5";
  static final String VARIABLE_MODELO = "ASISTENTE_MODELO";
  // Las consultas se cachean por usuario y mes; esto es solo una red de seguridad de costo
  static final int MAXIMO_LLAMADAS_POR_DIA = 200;
  // Holgado: con 800 el JSON de la síntesis llegó cortado
  private static final long MAX_TOKENS = 2000L;
  // Largo de la columna donde se guarda la síntesis (Consejo.textoRedactado)
  static final int LARGO_MAXIMO = 1000;
  private static final int MAXIMO_CATEGORIAS_DE_CONTEXTO = 5;

  static final String INSTRUCCIONES =
    "Sos el asistente financiero de Gastitos, una app argentina de gastos personales. Recibís en " +
    "JSON el contexto de un período (variación de los gastos, inflación, categorías del último mes " +
    "cerrado) y una lista de conclusiones ya calculadas por reglas, cada una con su familia " +
    "(LECTURA: qué pasó; PROPUESTA: qué se propone hacer), su severidad y un texto con los hechos. " +
    "Escribí un solo párrafo, de tres a cinco oraciones, en español rioplatense con voseo, en tono " +
    "cercano y directo, que le cuente al usuario cómo le fue en el período conectando esas " +
    "conclusiones: empezá por el veredicto (cuánto subieron sus gastos, cuánto la inflación y cuánto " +
    "cambió en serio, con esos tres números), seguí por lo más importante de las lecturas y, si hay " +
    "propuestas, cerrá con lo que se propone hacer y su monto. Reglas: usá solo los hechos y números " +
    "que recibís, sin inventar, redondear distinto ni agregar recomendaciones o conclusiones propias; " +
    "no hace falta repetir todos los números, elegí los que hacen al relato; sin emojis, sin signos de " +
    "exclamación, sin títulos ni listas; no repitas el nombre de la app. Respondé únicamente con el " +
    "JSON pedido.";

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
  public String sintetizar(List<Consejo> consejos, GastoReal contexto) {
    if (!conClave || consejos.isEmpty() || !hayCupoHoy()) {
      return null;
    }
    try {
      SintesisDelPeriodo sintesis = consultar(armarHechos(consejos, contexto));
      if (sintesis == null || sintesis.texto() == null || sintesis.texto().isBlank()) {
        return null;
      }
      return recortarAlLargoDeLaColumna(sintesis.texto().trim());
    } catch (RuntimeException e) {
      // Sin red, clave inválida, respuesta rara: se muestra el veredicto de las reglas
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "No se pudo pedir la síntesis a Claude: {0}", e.getMessage());
      }
      return null;
    }
  }

  // Si el modelo se extiende de más, se corta en el último punto que entra en la columna
  static String recortarAlLargoDeLaColumna(String texto) {
    if (texto.length() <= LARGO_MAXIMO) {
      return texto;
    }
    int ultimoPunto = texto.lastIndexOf('.', LARGO_MAXIMO - 1);
    return ultimoPunto > 0 ? texto.substring(0, ultimoPunto + 1) : texto.substring(0, LARGO_MAXIMO);
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
      nodo.put("familia", consejo.getTipo().getFamilia().name());
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

  private SintesisDelPeriodo consultar(String hechos) {
    StructuredMessageCreateParams<SintesisDelPeriodo> params = MessageCreateParams
      .builder()
      .model(modelo)
      .maxTokens(MAX_TOKENS)
      .outputConfig(SintesisDelPeriodo.class)
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
