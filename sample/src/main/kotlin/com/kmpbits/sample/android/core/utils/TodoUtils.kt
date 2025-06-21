package com.kmpbits.sample.android.core.utils

enum class ListOperation { ADD, UPDATE, DELETE }

fun <T> modifyListById(
    list: List<T>,
    id: Int,
    operation: ListOperation,
    idSelector: (T) -> Int,
    newItem: T? = null,
): List<T> {
    return when (operation) {
        ListOperation.ADD -> {
            // Only add if newItem isn't null and no item with this id exists
            if (newItem != null && list.none { idSelector(it) == id })
                list + newItem
            else list
        }

        ListOperation.UPDATE -> {
            // Replace item if found; otherwise do nothing
            list.map { if (idSelector(it) == id) newItem ?: it else it }
        }

        ListOperation.DELETE -> {
            list.filter { idSelector(it) != id }
        }
    }
}
