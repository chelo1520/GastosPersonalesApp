-- Se ejecuta en cada arranque: solo inserta el usuario de prueba si todavía no existe
INSERT INTO Usuario(email, password, rol, activo) SELECT 'test@unlam.edu.ar', 'test', 'ADMIN', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE email = 'test@unlam.edu.ar');

INSERT INTO Categoria(nombre, descripcion) SELECT 'Alimentación', 'Comidas y compras de alimentos' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Alimentación');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Transporte', 'Viajes y movilidad' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Transporte');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Vivienda', 'Alquiler, expensas y mantenimiento' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Vivienda');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Servicios', 'Luz, gas, agua, internet y telefonía' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Servicios');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Salud', 'Gastos médicos, farmacia y cuidado personal' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Salud');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Educación', 'Estudios, cursos y materiales' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Educación');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Entretenimiento', 'Salidas, actividades y suscripciones' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Entretenimiento');
INSERT INTO Categoria(nombre, descripcion) SELECT 'Otros', 'Gastos que no corresponden a otra categoría' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Otros');
