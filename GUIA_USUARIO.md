# Guía de uso de Nummia

## Para qué sirve

Nummia permite llevar un control personal de ingresos, gastos, saldo, pagos pendientes y límites de gasto. Los datos se guardan en el dispositivo y permanecen después de cerrar la aplicación.

## Inicio

La pantalla **Inicio** muestra el saldo actual, el saldo inicial, los ingresos y gastos del mes, el progreso de los presupuestos que tengan un límite, los próximos pagos y los movimientos recientes. Los periodos sin límite no aparecen en este resumen.

- La primera vez aparece la configuración de bienvenida. Elige el saldo inicial, la moneda, la paleta y si quieres activar los recordatorios dentro de la app y las notificaciones del móvil. La app guarda estas opciones y después abre Inicio; podrás cambiarlas en Ajustes.
- Pulsa **Editar** junto al saldo inicial para establecer el dinero que tenías al empezar.
- Pulsa **Añadir movimiento** para registrar un ingreso o un gasto. Indica una descripción, el importe y una categoría.
- Pulsa **Añadir** en Próximos pagos para anotar una factura única o recurrente, con su importe, categoría y fecha o día de facturación. Las recurrentes pueden ser semanales o mensuales; opcionalmente, puedes limitar la repetición a una semana o mes final e indicar si se incluye ese último periodo.

El saldo actual se calcula con el saldo inicial más los ingresos, menos los gastos registrados. Los pagos previstos no se restan del saldo hasta que registres el pago como gasto.

Las facturas vencidas aparecen marcadas como **RETRASADA**. Los recordatorios elegibles se agrupan en un único aviso encima de la lista de facturas en Inicio y Presupuesto; al iniciar la app también aparece una ventana con las facturas próximas o vencidas. Las facturas para las que se eligió **No recordar** no aparecen en esos avisos. En **Ajustes** puedes activar o desactivar todos los recordatorios, o desmarcar **Notificaciones del móvil** para desactivar solo las notificaciones del sistema y conservar los avisos dentro de la app. También puedes configurar los días de antelación para facturas mensuales y semanales, la hora, cada cuánto se repiten los avisos y cuánto dura **Más tarde**. Los valores iniciales son 7 días antes para las mensuales, 2 para las semanales, las 9:00, repetición diaria y posposición de 24 horas. Los avisos continúan hasta marcar la factura como pagada. En cada notificación o aviso de la app puedes abrir los detalles, posponerla o dejar de recibir recordatorios para esa factura. En Android 13 o posterior, la app solicita el permiso de notificaciones al activar la opción durante la configuración inicial o en Ajustes. Si las notificaciones están bloqueadas en Android, los avisos dentro de la app siguen disponibles.

## Movimientos

La pestaña **Movimientos** muestra por defecto los movimientos del mes actual, ordenados del más reciente al más antiguo y agrupados por mes y semana. Pulsa **Mostrar el mes anterior** al final para ampliar el historial mes a mes; el botón desaparece cuando ya no hay movimientos anteriores. Usa el buscador para localizar por palabras en título, categoría o descripción; no distingue mayúsculas ni tildes, permite términos en cualquier orden y tolera una errata sencilla. También busca por importe, tipo de movimiento y fecha. Puedes filtrar con los botones horizontales de categoría y seleccionar fechas **Desde** y **Hasta** (ambos días se incluyen). El buscador, la categoría y el intervalo de fechas se pueden combinar y consultan todo el historial. Pulsa un movimiento para consultar o editar sus datos o eliminarlo (la app pedirá confirmación). Usa **Registrar movimiento** para añadir uno nuevo.

Las categorías disponibles son Comida, Ocio, Transporte, Facturas, Vivienda, Salud, Trabajo y Otros.

## Presupuesto

La pestaña **Presupuesto** compara el gasto de la semana y del mes con sus límites. Pulsa **Editar** para cambiar los importes semanal y mensual o marca **Sin límite semanal/mensual** para desactivar cada límite por separado. Los periodos sin límite se muestran aquí como **Sin límite**, junto con el gasto acumulado; no aparecen en el resumen de Inicio. En **Pagos pendientes** puedes añadir facturas con una descripción opcional. Pulsa una factura para ver sus detalles, editarla o eliminarla (con confirmación). Pulsa **Marcar pagada** cuando hayas realizado un pago. Antes de confirmarlo puedes añadir un extra por retraso. Solo al confirmar se registra el importe total como gasto; las facturas recurrentes conservan el siguiente vencimiento y las de una sola vez desaparecen de pendientes.

El límite semanal considera la semana de lunes a domingo. El límite mensual considera el mes natural actual.

## Estadísticas

La pestaña **Estadísticas** muestra los ingresos y gastos registrados durante el mes actual, el desglose de gastos por categoría y una comparación de los gastos del mes actual con los del mes anterior. Pulsa las tarjetas de ingresos o gastos para abrir un gráfico de líneas; desde Inicio puedes hacer lo mismo con sus tarjetas del mes. Pulsa el saldo de Inicio para abrir la vista general, con ingresos y gastos juntos en la misma gráfica. Los gráficos permiten elegir últimos 7 días, este mes, últimos 3 meses, últimos 6 meses, último año o todo el histórico, y alternar importes por periodo o acumulados. Debajo, las barras muestran el importe y porcentaje por categoría del periodo seleccionado. Toca o desliza sobre el gráfico para actualizar el detalle al punto marcado; en modo acumulado se considera desde el inicio del periodo hasta ese punto. Pulsa una categoría del desglose para abrir su gráfico. La comparativa mensual también se puede tocar para abrir el detalle comparativo; allí puedes elegir otro mes.

## Ajustes

Pulsa el icono de ajustes situado a la derecha del título de una sección principal para abrir esta página. En **Moneda** puedes elegir entre euros, dólares, libras y otras monedas. El cambio no convierte los importes con tipos de cambio: conserva sus valores numéricos y, si la moneda no usa decimales, los redondea a la unidad más cercana.

Además, en **Paleta** puedes elegir entre varias combinaciones visuales para personalizar la aplicación. Las disponibles incluyen:

- Actual (Azul claro)
- Azul profundo
- Azul suave
- Azul primario
- Azul petróleo
- Verde financiero
- Lila premium
- Gris elegante
- Azul claro corporativo
- Naranja financiero
- Amarillo cálido
- Rojo coral
- Negro / gris oscuro
- Blanco / gris claro

La paleta elegida se guarda localmente y se mantiene al volver a abrir la aplicación.

En Ajustes > Actualizaciones puedes activar o desactivar la detección automática. Cuando está activa, la app busca al iniciarse, como máximo una vez cada 24 horas, si hay una versión más reciente en las Releases públicas de GitHub. Si encuentra una, primero pregunta si quieres descargarla; una vez verificada, vuelve a preguntar antes de abrir el instalador de Android. También puedes buscar versiones manualmente aunque desactives la detección automática. Android puede pedir permiso para instalar aplicaciones desde Nummia y siempre solicita confirmar la instalación. La actualización debe estar firmada con el mismo certificado y conservar el mismo identificador de aplicación; de lo contrario Android la rechazará.

## Datos y limitaciones

- En una instalación nueva, la app empieza sin movimientos ni pagos de ejemplo para que el saldo inicial configurado no se mezcle con datos ficticios. Las instalaciones actualizadas conservan sus datos.
- Si la app se cierra inesperadamente, al volver a abrirla ofrece copiar o compartir un informe con otra aplicación. El informe queda guardado solo en el dispositivo y no se envía hasta que elijas compartirlo.
- Los datos se almacenan localmente en este dispositivo; la app no los sincroniza con una cuenta o servicio en la nube.
- Los movimientos se pueden editar o eliminar desde su detalle. Las facturas pendientes se pueden añadir y marcar como pagadas; no se descuentan automáticamente y pueden generar avisos y recordatorios.
- Los errores al guardar se muestran en un mensaje en pantalla.
