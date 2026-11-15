# Changelog

## 0.8 - Primera versión

Primera versión de Nummia, una aplicación Android para llevar las finanzas personales desde el dispositivo.

### Funcionalidades

- **Inicio y saldo:** consulta del saldo disponible, configuración del saldo inicial y resumen mensual de ingresos y gastos.
- **Movimientos:** registro de ingresos y gastos con título, descripción opcional, importe, categoría y fecha/hora; edición y eliminación con confirmación.
- **Historial:** lista cronológica agrupada por mes y semana, con carga progresiva de meses anteriores.
- **Búsqueda y filtros:** búsqueda por texto, importe, tipo y fecha; ignora mayúsculas y tildes, acepta los términos en cualquier orden y tolera una errata sencilla. Se puede combinar con filtros por categoría y un intervalo de fechas inclusivo.
- **Presupuestos:** límites semanales y mensuales independientes, con opción de desactivar cada límite. La semana presupuestaria va de lunes a domingo y el mes corresponde al mes natural.
- **Facturas y pagos previstos:** creación de pagos únicos o recurrentes (semanales o mensuales), con categoría, descripción opcional y fecha o día de vencimiento. Las recurrencias pueden tener un periodo final configurable.
- **Registro de pagos:** marcar una factura como pagada registra el gasto; permite añadir un extra por retraso. Las facturas recurrentes avanzan al siguiente vencimiento y las de pago único dejan la lista de pendientes.
- **Recordatorios locales:** avisos dentro de Inicio y Presupuesto y notificaciones del sistema para pagos próximos o vencidos. Se pueden configurar la antelación, la hora, la repetición y la posposición; cada aviso permite abrir los detalles, posponerlo o desactivar los recordatorios de esa factura.
- **Estadísticas:** resúmenes mensuales, gastos por categoría y comparativas entre meses. Incluye gráficos por periodo o acumulados para ingresos, gastos, una vista conjunta y categorías; al seleccionar un punto se actualizan los totales y el desglose.
- **Moneda:** selección de distintas monedas y formato acorde con sus decimales. Cambiar de moneda no convierte los importes mediante tipos de cambio.
- **Personalización:** varias paletas visuales, incluida la vista previa en Ajustes.
- **Persistencia local:** saldo, movimientos, facturas, presupuestos, moneda y paleta se guardan en una base de datos local. No requiere cuenta y no sincroniza datos con la nube.
- **Inicio sin datos precargados:** la aplicación comienza con saldo cero y sin movimientos, facturas ni presupuestos; los datos que aparecen son los que añade el usuario.

### Detalles técnicos

- Aplicación nativa para Android con Kotlin, Jetpack Compose y Material 3.
- Compatible desde Android 8 (API 26).
- Los importes se almacenan en céntimos para evitar errores de redondeo.
- Las notificaciones del sistema requieren el permiso correspondiente en Android 13 o posterior.
