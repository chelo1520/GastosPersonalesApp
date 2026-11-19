package com.tallerwebi.dominio.comprobante;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import java.util.List;

/** Los comprobantes de los que se cargan gastos: que no se repitan y que quede el archivo. */
public interface ServicioComprobante {
  // Lanza ComprobanteYaCargado si el usuario ya cargó este archivo y sus gastos siguen guardados
  void verificarQueNoEsteCargado(ArchivoComprobante archivo, Usuario usuario);

  // Guarda los gastos (todos o ninguno) ligados al comprobante y sube el archivo al almacén. Si el
  // almacén no está o falla, los gastos se guardan igual: el archivo es un respaldo, no un requisito
  void registrarGastos(List<Gasto> gastos, ArchivoComprobante archivo, Usuario usuario);

  // Link temporal al archivo del comprobante. Lanza ComprobanteNoEncontrado si no es del usuario,
  // no tiene archivo guardado o el almacén no está disponible
  String urlDelArchivo(Long idComprobante, Usuario usuario);

  // Las claves de los archivos del usuario, para borrarlos después de eliminar su cuenta
  List<String> archivosDe(Usuario usuario);

  // Borra esos archivos del almacén; si alguno falla, sigue con los demás
  void eliminarArchivos(List<String> claves);
}
