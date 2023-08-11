package com.github.quillraven.fleks.benchmark

import com.artemis.ComponentMapper
import com.artemis.WorldConfigurationBuilder
import com.artemis.annotations.All
import com.artemis.annotations.Exclude
import com.artemis.annotations.One
import com.artemis.systems.IteratingSystem
import dev.dominion.ecs.api.Dominion
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

data class DominionPosition(var x: Float = 0f, var y: Float = 0f)

data class DominionLife(var life: Float = 0f)

data class DominionSprite(var path: String = "", var animationTime: Float = 0f)

class DominionSystemSimple(var world: Dominion) : Runnable {
    override fun run() {
        //finds entities
        world.findEntitiesWith(DominionPosition::class.java).stream().forEach{
            it.comp.x++
        }
    }
}

@All(DominionPosition::class)
@Exclude(DominionLife::class)
@One(DominionSprite::class)
class DominionSystemComplex1(var world: Dominion) : Runnable {
    private var processCalls = 0
//    private lateinit var positions: ComponentMapper<DominionPosition>
//    private lateinit var lifes: ComponentMapper<DominionLife>
//    private lateinit var sprites: ComponentMapper<DominionSprite>

    override fun process(entityId: Int) {
        if (processCalls % 2 == 0) {
            positions[entityId].x++
            lifes.create(entityId)
        } else {
            positions.remove(entityId)
        }
        sprites[entityId].animationTime++
        ++processCalls
    }

    override fun run() {
        world.composition().byAdding1AndRemoving(DominionPosition::class.java, DominionLife::class.java)
        //finds entities
        world.findEntitiesWith().stream().forEach{
            it.comp.x++
        }
    }

}

@One(DominionPosition::class, DominionLife::class, DominionSprite::class)
class DominionSystemComplex2 : IteratingSystem() {
    private lateinit var positions: ComponentMapper<DominionPosition>
    private lateinit var lifes: ComponentMapper<DominionLife>

    override fun process(entityId: Int) {
        lifes.remove(entityId)
        positions.create(entityId)
    }
}

@State(Scope.Benchmark)
open class DominionStateAddRemove {
    lateinit var world: Dominion

    @Setup(value = Level.Iteration)
    fun setup() {
        world = Dominion.create()
    }
}

@State(Scope.Benchmark)
open class DominionStateSimple {
    lateinit var world: Dominion

    @Setup(value = Level.Iteration)
    fun setup() {
        world = Dominion.create()

        repeat(NUM_ENTITIES) {
            world.createEntity().edit().create(DominionPosition::class.java)
        }
    }
}

@State(Scope.Benchmark)
open class DominionStateComplex {
    lateinit var world: Dominion

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World(WorldConfigurationBuilder().run {
            with(DominionSystemComplex1())
            with(DominionSystemComplex2())
            build()
        })

        repeat(NUM_ENTITIES) {
            val entityEdit = world.createEntity().edit()
            entityEdit.create(DominionPosition::class.java)
            entityEdit.create(DominionSprite::class.java)
        }
    }
}

@Fork(value = WARMUPS)
@Warmup(iterations = WARMUPS)
@Measurement(iterations = ITERATIONS, time = TIME, timeUnit = TimeUnit.SECONDS)
open class DominionBenchmark {
    @Benchmark
    fun addRemove(state: DominionStateAddRemove) {
        repeat(NUM_ENTITIES) {
            state.world.createEntity().edit().create(DominionPosition::class.java)
        }
        repeat(NUM_ENTITIES) {
            state.world.delete(it)
        }
    }

    @Benchmark
    fun simple(state: DominionStateSimple) {
        repeat(WORLD_UPDATES) {
            state.world.delta = 1f
            state.world.process()
        }
    }

    @Benchmark
    fun complex(state: DominionStateComplex) {
        repeat(WORLD_UPDATES) {
            state.world.delta = 1f
            state.world.process()
        }
    }
}
