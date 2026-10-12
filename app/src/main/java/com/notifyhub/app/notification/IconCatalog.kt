package com.notifyhub.app.notification

import com.notifyhub.app.R

/**
 * The fixed set of icons a user can assign to a registered project.
 * Each icon is a bundled monochrome vector drawable, used both:
 *  - in the app UI (tinted with the project's chosen color), and
 *  - as the small icon of the actual Android system notification.
 *
 * Icons are identified by a stable [key] that is persisted in Room, so adding
 * new icons later never breaks existing registrations.
 */
data class CatalogIcon(
    val key: String,
    val label: String,
    val drawableRes: Int
)

object IconCatalog {

    val icons: List<CatalogIcon> = listOf(
        CatalogIcon("bell", "Bell", R.drawable.ic_cat_bell),
        CatalogIcon("rocket", "Rocket", R.drawable.ic_cat_rocket),
        CatalogIcon("cloud", "Cloud", R.drawable.ic_cat_cloud),
        CatalogIcon("server", "Server", R.drawable.ic_cat_server),
        CatalogIcon("code", "Code", R.drawable.ic_cat_code),
        CatalogIcon("warning", "Warning", R.drawable.ic_cat_warning),
        CatalogIcon("check", "Check", R.drawable.ic_cat_check),
        CatalogIcon("bolt", "Bolt", R.drawable.ic_cat_bolt),
        CatalogIcon("chart", "Chart", R.drawable.ic_cat_chart),
        CatalogIcon("shield", "Shield", R.drawable.ic_cat_shield),
        CatalogIcon("star", "Star", R.drawable.ic_cat_star),
    )

    val default: CatalogIcon = icons.first()

    fun byKey(key: String): CatalogIcon = icons.firstOrNull { it.key == key } ?: default
}
