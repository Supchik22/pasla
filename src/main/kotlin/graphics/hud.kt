package io.github.supchik22.graphics

import io.github.supchik22.util.Window
import io.github.supchik22.world.entity.Player
import org.lwjgl.opengl.GL11.*

fun beginHUDRendering(windowWidth: Int, windowHeight: Int) {
    glMatrixMode(GL_PROJECTION)
    glPushMatrix()
    glLoadIdentity()
    glOrtho(0.0, windowWidth.toDouble(), windowHeight.toDouble(), 0.0, -1.0, 1.0)

    glMatrixMode(GL_MODELVIEW)
    glPushMatrix()
    glLoadIdentity()

    glDisable(GL_DEPTH_TEST)
}
fun drawRect(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float, a: Float) {
    glColor4f(r, g, b, a)
    glBegin(GL_QUADS)
    glVertex2f(x, y)
    glVertex2f(x + width, y)
    glVertex2f(x + width, y + height)
    glVertex2f(x, y + height)
    glEnd()
}
fun renderHUD(window: Window, player: Player) {
    val width = window.width
    val height = window.height

    beginHUDRendering(width, height)

    val healthPercent = player.getHealth() / player.getMaxHealth()

    drawRect(20f, 20f, 200f, 20f, 0.2f, 0.2f, 0.2f, 1f) // фон
    drawRect(20f, 20f, 200f * healthPercent, 20f, 1f, 0f, 0f, 1f) // hp

    endHUDRendering()
}

fun endHUDRendering() {
    glEnable(GL_DEPTH_TEST)

    glMatrixMode(GL_PROJECTION)
    glPopMatrix()

    glMatrixMode(GL_MODELVIEW)
    glPopMatrix()
}
