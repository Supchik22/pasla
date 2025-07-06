package io.github.supchik22.world.entity

interface Damageable {
    fun takeDamage(amount: Float)
    fun heal(amount: Float)
    fun isAlive(): Boolean
    fun getHealth(): Float
    fun getMaxHealth(): Float
    fun setHealth(amount: Float)
    fun setMaxHealth(amount: Float)
    fun onDeath()
}
