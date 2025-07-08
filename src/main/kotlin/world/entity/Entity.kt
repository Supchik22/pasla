package io.github.supchik22.world.entity

import io.github.supchik22.rendering.Renderable


open class Entity(
    private var maxHealth: Float = 100f
) : Damageable, Renderable {

    private var health: Float = 50f
    private var alive: Boolean = true

    override fun takeDamage(amount: Float) {
        if (!alive) return
        health -= amount
        if (health <= 0f) {
            health = 0f
            alive = false
            onDeath()
        }
    }

    override fun heal(amount: Float) {
        if (!alive) return
        health = (health + amount).coerceAtMost(maxHealth)
    }

    override fun isAlive(): Boolean = alive

    override fun getHealth(): Float = health

    override fun getMaxHealth(): Float = maxHealth

    override fun setHealth(amount: Float) {
        health = amount.coerceIn(0f, maxHealth)
        alive = health > 0f
        if (!alive) onDeath()
    }

    override fun setMaxHealth(amount: Float) {
        maxHealth = amount
        health = health.coerceAtMost(maxHealth)
    }

    override fun onDeath() {

    }
}
