package app

const val MAX_SIDE_PANEL_ITEMS_HEIGHT = 300
const val ENTRY_HEIGHT = 40

fun maxSidePanelHeight(itemsCount: Int): Int {
    val itemsHeight = itemsCount * ENTRY_HEIGHT

    return if (itemsHeight < MAX_SIDE_PANEL_ITEMS_HEIGHT)
        itemsHeight
    else
        MAX_SIDE_PANEL_ITEMS_HEIGHT
}