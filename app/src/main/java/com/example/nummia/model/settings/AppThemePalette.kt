package com.example.nummia.model.settings
/** Define los temas disponibles para personalizar la interfaz. */
enum class AppThemePalette(
    val id: String,
    val label: String
) {
    DEFAULT("default", "Azul claro"),
    MIDNIGHT("midnight", "Azul profundo"),
    SOFT("soft", "Azul suave"),
    CLASSIC("classic", "Azul primario"),
    PETROLEUM("petroleum", "Azul petróleo"),
    GREEN("green", "Verde financiero"),
    LILAC("lilac", "Lila premium"),
    GREY("grey", "Gris elegante"),
    BLUE("blue", "Azul claro corporativo"),
    ORANGE("orange", "Naranja financiero"),
    YELLOW("yellow", "Amarillo cálido"),
    RED("red", "Rojo coral"),
    CHARCOAL("charcoal", "Negro / gris oscuro"),
    LIGHT("light", "Blanco / gris claro");

    companion object {
        /** Recupera la paleta guardada y usa la predeterminada si el identificador no existe. */
        fun from(value: String?): AppThemePalette =
            entries.firstOrNull { it.id == value } ?: DEFAULT
    }
}
