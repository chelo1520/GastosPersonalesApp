package com.tallerwebi.infraestructura.comprobante;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Convierte el JSON que escribió el modelo en los gastos propuestos del comprobante. */
@Component
public class ParserComprobante {

  // Un resumen de tarjeta largo se corta acá; el resto se carga a mano
  static final int MAXIMO_DE_GASTOS = 40;
  // Largo de las columnas de texto del gasto
  static final int LARGO_MAXIMO_TEXTO = 255;
  private static final String CONFIANZA_ALTA = "alta";
  private static final String CONFIANZA_MEDIA = "media";

  private final ObjectMapper json = new ObjectMapper();

  public DatosComprobante parsear(String jsonDelModelo) {
    NodoJson datos = leer(jsonDelModelo);
    String tipoOperacion = datos.texto("tipoOperacion", LARGO_MAXIMO_TEXTO);
    String tipoComprobante = datos.texto("tipoComprobante", LARGO_MAXIMO_TEXTO);
    if (tipoOperacion == null || tipoComprobante == null) {
      throw new ComprobanteNoLegible();
    }

    List<GastoDelComprobante> gastos = new ArrayList<>();
    for (NodoJson gasto : datos.elementos("gastos")) {
      GastoDelComprobante leido = parsearGasto(gasto);
      if (leido != null && gastos.size() < MAXIMO_DE_GASTOS) {
        gastos.add(leido);
      }
    }
    if (gastos.isEmpty()) {
      throw new ComprobanteNoLegible();
    }
    return new DatosComprobante(tipoOperacion, tipoComprobante, datos.importe("total"), gastos);
  }

  // Un renglón sin importe o sin comercio se descarta: la revisión compara la suma con el total
  // del comprobante, así que la diferencia queda a la vista. Sin fecha, la completa la persona
  GastoDelComprobante parsearGasto(NodoJson gasto) {
    Double importe = gasto.importe("importe");
    String descripcion = gasto.texto("descripcion", LARGO_MAXIMO_TEXTO);
    if (importe == null || descripcion == null) {
      return null;
    }
    LocalDate fecha = gasto.fecha("fecha");
    return new GastoDelComprobante(
      importe,
      fecha,
      descripcion,
      gasto.texto("detalle", LARGO_MAXIMO_TEXTO),
      gasto.texto("categoria", LARGO_MAXIMO_TEXTO),
      confianza(gasto.hijo("confianza"), fecha == null)
    );
  }

  // Una fecha que no se pudo leer siempre se marca para revisar
  ConfianzaComprobante confianza(NodoJson confianza, boolean faltaLaFecha) {
    return new ConfianzaComprobante(
      esBaja(confianza, "importe"),
      faltaLaFecha || esBaja(confianza, "fecha"),
      esBaja(confianza, "descripcion"),
      esBaja(confianza, "categoria")
    );
  }

  // Solo "alta" y "media" valen como confiables; cualquier otra cosa (o nada) es baja
  private boolean esBaja(NodoJson confianza, String campo) {
    String nivel = confianza.texto(campo, LARGO_MAXIMO_TEXTO);
    return !CONFIANZA_ALTA.equalsIgnoreCase(nivel) && !CONFIANZA_MEDIA.equalsIgnoreCase(nivel);
  }

  private NodoJson leer(String jsonDelModelo) {
    try {
      return new NodoJson(json.readTree(jsonDelModelo));
    } catch (JsonProcessingException e) {
      throw new ComprobanteNoLegible(e);
    }
  }
}
