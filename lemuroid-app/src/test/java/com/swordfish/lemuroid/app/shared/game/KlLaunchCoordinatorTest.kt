package com.swordfish.lemuroid.app.shared.game

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class KlLaunchCoordinatorTest {
    @Test fun concurrentTapsStartOnlyOneLaunch() {
        val coordinator = KlLaunchCoordinator()
        val pool = Executors.newFixedThreadPool(8)
        try {
            val starts = pool.invokeAll((1..100).map { Callable { coordinator.begin("Pikmin") } })
            assertEquals(1, starts.count { it.get() != null })
            assertEquals("Pikmin", coordinator.status.value?.title)
        } finally { pool.shutdownNow() }
    }
    @Test fun failedPreparationKeepsVisibleErrorAndAllowsRetry() {
        val coordinator = KlLaunchCoordinator()
        val first = coordinator.begin("Pikmin")!!
        coordinator.fail(first, "Não foi possível preparar os arquivos de sistema.")
        assertTrue(coordinator.status.value!!.error)
        assertTrue(coordinator.status.value!!.message.contains("arquivos de sistema"))
        val second = coordinator.begin("Pikmin")!!
        assertNotEquals(first, second)
        assertFalse(coordinator.status.value!!.error)
        coordinator.complete(second)
        assertNull(coordinator.status.value)
    }
    @Test fun staleCompletionCannotEraseAnotherAttempt() {
        val coordinator = KlLaunchCoordinator()
        val first = coordinator.begin("Pikmin")!!
        coordinator.fail(first, "Falha")
        val second = coordinator.begin("Pikmin")!!
        coordinator.progress(second, "Preparando o Dolphin")
        coordinator.complete(first)
        coordinator.fail(first, "Erro atrasado")
        assertEquals("Preparando o Dolphin", coordinator.status.value?.message)
        assertNull(coordinator.begin("Outro jogo"))
    }
    @Test fun busyLibraryHasVisibleDismissibleReason() {
        val coordinator = KlLaunchCoordinator()
        coordinator.reject("Pikmin", "Biblioteca em atualização")
        assertTrue(coordinator.status.value!!.error)
        coordinator.dismissError()
        assertNull(coordinator.status.value)
    }
}
