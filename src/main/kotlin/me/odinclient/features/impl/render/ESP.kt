package me.odinclient.features.impl.render

import me.odinclient.config.MiscConfig
import me.odinclient.events.impl.PostEntityMetadata
import me.odinclient.events.impl.RenderEntityModelEvent
import me.odinclient.features.Category
import me.odinclient.features.Module
import me.odinclient.features.settings.Setting.Companion.withDependency
import me.odinclient.features.settings.impl.*
import me.odinclient.utils.VecUtils.noSqrt3DDistance
import me.odinclient.utils.render.Color
import me.odinclient.utils.render.world.OutlineUtils
import me.odinclient.utils.skyblock.ChatUtils.modMessage
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ESP : Module(
    "ESP",
    category = Category.RENDER,
    tag = TagType.FPSTAX,
    description = "Allows you to highlight selected mobs."
) {
    private val scanDelay: Long by NumberSetting("Scan Delay", 500L, 100L, 2000L, 100L)
    val color: Color by ColorSetting("Color", Color(255, 0, 0), true)
    val mode: Int by SelectorSetting("Mode", "Outline", arrayListOf("Outline", "Overlay", "Both"))
    val xray: Boolean by BooleanSetting("Through Walls", true)
    private val thickness: Float by NumberSetting("Outline Thickness", 5f, 5f, 20f, 0.5f).withDependency { mode != 1 }
    private val cancelHurt: Boolean by BooleanSetting("Cancel Hurt", true).withDependency { mode != 1 }

    private val addStar: () -> Unit by ActionSetting("Add Star") {
        if (MiscConfig.espList.contains("✯")) return@ActionSetting
        modMessage("Added ✯ to ESP list")
        MiscConfig.espList.add("✯")
        MiscConfig.saveAllConfigs()
    }

    private inline val espList get() = MiscConfig.espList

    var currentEntities = mutableSetOf<Entity>()

    init {
        execute({ scanDelay }) {
            currentEntities.removeAll { it.isDead }
            getEntities()
        }

        execute(30_000) {
            currentEntities.clear()
            getEntities()
        }

        onWorldLoad { currentEntities.clear() }
    }

    @SubscribeEvent
    fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (mode == 1) return
        if (event.entity !in currentEntities) return
        if (!mc.thePlayer.canEntityBeSeen(event.entity) && !xray) return

        OutlineUtils.outlineEntity(
            event,
            thickness,
            color,
            cancelHurt
        )
    }

    @SubscribeEvent
    fun postMeta(event: PostEntityMetadata) {
        checkEntity(mc.theWorld.getEntityByID(event.packet.entityId) ?: return)
    }

    private fun getEntities() {
        mc.theWorld?.loadedEntityList?.filterIsInstance<EntityArmorStand>()?.filterNot {
            ent -> !espList.any { ent.name.contains(it, true) } || currentEntities.contains(ent)
        }?.forEach(::checkEntity)
    }

    private fun checkEntity(entity: Entity) {
        currentEntities.add(
            mc.theWorld.getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox.expand(1.0, 5.0, 1.0))
                .filter { it != null && it !is EntityArmorStand && it != mc.thePlayer }
                .minByOrNull { noSqrt3DDistance(it, entity) } ?: return
        )
    }
}
