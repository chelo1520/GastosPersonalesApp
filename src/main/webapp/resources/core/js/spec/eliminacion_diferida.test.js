import { DEMORA_DESHACER, eliminarConDeshacer } from "../eliminacion_diferida.js";

describe("Eliminar con Deshacer", () => {
  let programado;
  let cancelado;
  let ejecutar;
  let alDeshacer;

  // Reloj falso: guarda la función programada para dispararla a mano
  const reloj = {
    programar: (funcion, demora) => {
      programado = { funcion, demora };
      return 7;
    },
    cancelar: (id) => {
      cancelado = id;
    },
  };

  beforeEach(() => {
    programado = null;
    cancelado = null;
    ejecutar = jasmine.createSpy("ejecutar");
    alDeshacer = jasmine.createSpy("alDeshacer");
  });

  it("elimina de verdad recién cuando pasan los 4 segundos", () => {
    const eliminacion = eliminarConDeshacer({ ejecutar, alDeshacer, ...reloj });

    expect(programado.demora).toBe(DEMORA_DESHACER);
    expect(ejecutar).not.toHaveBeenCalled();
    expect(eliminacion.estaPendiente()).toBeTrue();

    programado.funcion();

    expect(ejecutar).toHaveBeenCalledTimes(1);
    expect(eliminacion.estaPendiente()).toBeFalse();
  });

  it("deshacer antes de tiempo cancela la eliminación", () => {
    const eliminacion = eliminarConDeshacer({ ejecutar, alDeshacer, ...reloj });

    expect(eliminacion.deshacer()).toBeTrue();

    expect(alDeshacer).toHaveBeenCalledTimes(1);
    expect(cancelado).toBe(7);

    programado.funcion();

    expect(ejecutar).not.toHaveBeenCalled();
  });

  it("confirmar adelanta la eliminación una sola vez y después ya no se puede deshacer", () => {
    const eliminacion = eliminarConDeshacer({ ejecutar, alDeshacer, ...reloj });

    expect(eliminacion.confirmar()).toBeTrue();
    expect(eliminacion.confirmar()).toBeFalse();
    expect(eliminacion.deshacer()).toBeFalse();

    expect(ejecutar).toHaveBeenCalledTimes(1);
    expect(alDeshacer).not.toHaveBeenCalled();
  });
});
