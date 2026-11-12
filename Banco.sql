-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3306
-- Tiempo de generación: 11-11-2026 a las 04:07:21
-- Versión del servidor: 8.0.45
-- Versión de PHP: 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `banco_udb`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cliente`
--

DROP TABLE IF EXISTS `cliente`;
CREATE TABLE IF NOT EXISTS `cliente` (
  `id_cliente` int NOT NULL AUTO_INCREMENT,
  `codigo_cliente` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dui` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_completo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `telefono` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('Activo','Inactivo','Bloqueado') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Activo',
  PRIMARY KEY (`id_cliente`),
  UNIQUE KEY `codigo_cliente` (`codigo_cliente`),
  UNIQUE KEY `dui` (`dui`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `cliente`
--

INSERT INTO `cliente` (`id_cliente`, `codigo_cliente`, `dui`, `nombre_completo`, `telefono`, `correo`, `direccion`, `estado`) VALUES
(1, 'CLI-0001', '01234567-8', 'Juan Carlos Pérez López', '7000-0001', 'juan.perez@correo.com', 'San Salvador, Col. Escalón', 'Activo'),
(2, 'CLI-0002', '02345678-9', 'María Fernanda Gómez', '7000-0002', 'maria.gomez@correo.com', 'Santa Tecla, La Libertad', 'Activo'),
(3, 'CLI-0003', '03456789-0', 'Carlos Alberto Martínez', '7000-0003', 'carlos.m@correo.com', 'Soyapango, San Salvador', 'Activo'),
(4, 'CLI-0004', '04567890-1', 'Ana Lucía Hernández', '7000-0004', 'ana.h@correo.com', 'Santa Ana, Centro', 'Activo'),
(7, 'CLI-0005', '01234567-3', 'Jose Mendez', '7055-5312', 'josemendez@gmail.com', 'Cojutepeque, Cuscatlan', 'Inactivo'),
(9, 'CLI-0006', '02234569-7', 'Jonathan Felix Segura Reyes', '7599-2366', 'jonafelixsegura@gmail.com', 'San Martin, El salvador', 'Activo'),
(10, 'CLI-0007', '05234889-9', 'Orlando Florida Fernandez', '2325-5596', 'orlado@gmail.com', 'Cojutepeque, Cuscatlan', 'Activo');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cuenta_bancaria`
--

DROP TABLE IF EXISTS `cuenta_bancaria`;
CREATE TABLE IF NOT EXISTS `cuenta_bancaria` (
  `id_cuenta` int NOT NULL AUTO_INCREMENT,
  `numero_cuenta` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_cliente` int NOT NULL,
  `tipo_cuenta` enum('Ahorro','Corriente') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Ahorro',
  `saldo` decimal(12,2) NOT NULL DEFAULT '0.00',
  `fecha_apertura` date NOT NULL DEFAULT (curdate()),
  `estado` enum('Activa','Inactiva','Bloqueada') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Activa',
  PRIMARY KEY (`id_cuenta`),
  UNIQUE KEY `numero_cuenta` (`numero_cuenta`),
  KEY `fk_cuenta_cliente` (`id_cliente`)
) ;

--
-- Volcado de datos para la tabla `cuenta_bancaria`
--

INSERT INTO `cuenta_bancaria` (`id_cuenta`, `numero_cuenta`, `id_cliente`, `tipo_cuenta`, `saldo`, `fecha_apertura`, `estado`) VALUES
(1, '100000000001', 1, 'Ahorro', 500.00, '2026-10-23', 'Bloqueada'),
(2, '100000000002', 1, 'Corriente', 1525.50, '2026-10-23', 'Activa'),
(3, '100000000003', 2, 'Ahorro', 250.75, '2026-10-23', 'Activa'),
(4, '100000000004', 9, 'Corriente', 2999.00, '2026-10-23', 'Activa'),
(5, '100000000005', 4, 'Ahorro', 57.99, '2026-10-31', 'Activa'),
(8, '100000000006', 9, 'Ahorro', 299.60, '2026-10-31', 'Activa'),
(9, '100000000007', 3, 'Ahorro', 199.00, '2026-10-31', 'Activa'),
(10, '100000000008', 10, 'Ahorro', 300.00, '2026-10-31', 'Inactiva');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `movimiento`
--

DROP TABLE IF EXISTS `movimiento`;
CREATE TABLE IF NOT EXISTS `movimiento` (
  `id_movimiento` int NOT NULL AUTO_INCREMENT,
  `id_cuenta` int NOT NULL,
  `tipo_movimiento` enum('Depósito','Retiro','Transferencia') COLLATE utf8mb4_unicode_ci NOT NULL,
  `monto` decimal(12,2) NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `descripcion` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `saldo_resultante` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id_movimiento`),
  KEY `fk_movimiento_cuenta` (`id_cuenta`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `movimiento`
--

INSERT INTO `movimiento` (`id_movimiento`, `id_cuenta`, `tipo_movimiento`, `monto`, `fecha_hora`, `descripcion`, `saldo_resultante`) VALUES
(1, 2, 'Depósito', 25.00, '2026-11-11 03:18:07', 'Pago de internet', 1525.50),
(2, 4, 'Retiro', 500.00, '2026-11-11 03:18:49', 'Pago de alquiler', 1000.00),
(3, 8, 'Depósito', 299.60, '2026-11-11 03:37:33', 'Abono a cuenta de ahorro', 299.60),
(4, 5, 'Depósito', 22.99, '2026-11-11 03:55:38', 'Ahorro mensual', 57.99),
(5, 4, 'Retiro', 1000.00, '2026-11-11 04:05:18', 'Mora', 0.00),
(6, 4, 'Depósito', 2999.00, '2026-11-11 04:05:40', 'Pago de vehiculo', 2999.00);

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `cuenta_bancaria`
--
ALTER TABLE `cuenta_bancaria`
  ADD CONSTRAINT `fk_cuenta_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id_cliente`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `movimiento`
--
ALTER TABLE `movimiento`
  ADD CONSTRAINT `fk_movimiento_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cuenta_bancaria` (`id_cuenta`) ON DELETE RESTRICT ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
