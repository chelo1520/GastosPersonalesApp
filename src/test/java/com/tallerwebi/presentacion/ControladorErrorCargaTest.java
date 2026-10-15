package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.dominio.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

public class ControladorErrorCargaTest {

  private final ControladorErrorCarga controlador = new ControladorErrorCarga();

  @Test
  public void debePedirOtraImagenCuandoElContenedorRechazaUnaCargaMayorA5Mb() {
    MockHttpServletRequest request = requestConUsuario();
    MultipartException error = new MultipartException(
      "Carga excedida",
      new MaxUploadSizeExceededException(5L * 1024 * 1024)
    );

    ModelAndView resultado = controlador.manejarErrorMultipart(error, request);

    assertEquals("cargar-comprobante", resultado.getViewName());
    assertEquals(
      "La imagen supera los 5 MB. Elegí otra imagen.",
      resultado.getModel().get("error")
    );
  }

  @Test
  public void debeRedirigirAlLoginCuandoElErrorDeCargaOcurreSinSesion() {
    ModelAndView resultado = controlador.manejarErrorMultipart(
      new MultipartException("Carga inválida"),
      new MockHttpServletRequest()
    );

    assertEquals("redirect:/login", resultado.getViewName());
  }

  private MockHttpServletRequest requestConUsuario() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO", new Usuario());
    request.setSession(session);
    return request;
  }
}
