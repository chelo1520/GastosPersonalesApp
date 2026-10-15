package com.tallerwebi.presentacion;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

/** Muestra un error recuperable cuando el contenedor rechaza un archivo multipart demasiado grande. */
@ControllerAdvice
public class ControladorErrorCarga {

  @ExceptionHandler(MultipartException.class)
  public ModelAndView manejarErrorMultipart(MultipartException error, HttpServletRequest request) {
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView("redirect:/login");
    }

    String mensaje = contieneLimiteExcedido(error)
      ? "El archivo supera los 5 MB. Elegí otro."
      : "No se pudo recibir el archivo. Elegí otro JPG, PNG o PDF.";
    ModelAndView vista = new ModelAndView("cargar-comprobante");
    vista.addObject("titulo", "Desde comprobante");
    vista.addObject("seccion", "registrar");
    vista.addObject("urlCancelar", "/registrar-gasto");
    vista.addObject("error", mensaje);
    return vista;
  }

  private boolean contieneLimiteExcedido(Throwable error) {
    for (Throwable causa = error; causa != null; causa = causa.getCause()) {
      if (causa instanceof MaxUploadSizeExceededException) {
        return true;
      }
    }
    return false;
  }
}
