export function validarCamposDeLogin(inputEmailValue, inputPasswordValue) {
  return inputEmailValue?.length > 0 && inputPasswordValue?.length > 0;
}

// Las mismas reglas que ReglasDeContrasena (Java). Especial: ni letra, ni número, ni espacio
export function reglasDeContrasena(password = "") {
  return {
    largo: password.length >= 6,
    mayuscula: /\p{Lu}/u.test(password),
    minuscula: /\p{Ll}/u.test(password),
    especial: /[^\p{L}\p{N}\s]/u.test(password),
  };
}

// Contraseña nueva (registro y cambio de contraseña): cumple todas las reglas y está repetida igual
export function validarPasswordNueva(password, confirmacion) {
  return Object.values(reglasDeContrasena(password)).every(Boolean) && password === confirmacion;
}

// Registro: email cargado y una contraseña nueva válida
export function validarCamposDeRegistro(email, password, confirmacion) {
  return email?.length > 0 && validarPasswordNueva(password, confirmacion);
}
