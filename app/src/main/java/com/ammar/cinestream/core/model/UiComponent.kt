package com.ammar.cinestream.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("component")
sealed class UiComponent {
    abstract val modifier: UiModifier?
}

@Serializable
data class UiModifier(
    val width: String? = "wrap",
    val height: String? = "wrap",
    val padding: Int? = 0,
    val backgroundColor: String? = null,
    val cornerRadius: Int? = 0
)

@Serializable
@SerialName("column")
data class UiColumn(
    val children: List<UiComponent>,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("row")
data class UiRow(
    val children: List<UiComponent>,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("box")
data class UiBox(
    val children: List<UiComponent>,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("lazy_row")
data class UiLazyRow(
    val children: List<UiComponent>,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("text")
data class UiText(
    val text: String,
    val style: String? = "body",
    val color: String? = null,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("image")
data class UiImage(
    val url: String,
    val aspectRatio: Float? = 1.0f,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("spacer")
data class UiSpacer(
    val size: Int,
    override val modifier: UiModifier? = null
) : UiComponent()

@Serializable
@SerialName("card")
data class UiCard(
    val children: List<UiComponent>,
    val action: String? = null,
    override val modifier: UiModifier? = null
) : UiComponent()