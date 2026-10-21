package com.example.messenger.presentation.components.call

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.presentation.components.call.model.CallBarMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CallBarPresenterTest {

    @BeforeEach
    fun setUp() {
        CallBarPresenter.setFloatingEnabled(true)
        CallBarPresenter.reset()
    }

    @AfterEach
    fun tearDown() {
        CallBarPresenter.setFloatingEnabled(true)
        CallBarPresenter.reset()
    }

    @Test
    fun `bubble and video tile are reachable while floating is enabled`() {
        CallBarPresenter.minimizeToBubble()
        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BUBBLE)

        CallBarPresenter.showVideoTile()
        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.VIDEO)
    }

    @Test
    fun `bubble and video tile stay in bar mode while floating is disabled`() {
        CallBarPresenter.setFloatingEnabled(false)

        CallBarPresenter.minimizeToBubble()
        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BAR)

        CallBarPresenter.showVideoTile()
        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BAR)
    }

    @Test
    fun `disabling floating while a bubble is shown falls back to the bar`() {
        CallBarPresenter.minimizeToBubble()

        CallBarPresenter.setFloatingEnabled(false)

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BAR)
    }

    @Test
    fun `restoring a hidden bubble with floating disabled shows the bar`() {
        CallBarPresenter.minimizeToBubble()
        CallBarPresenter.hide()
        CallBarPresenter.setFloatingEnabled(false)

        CallBarPresenter.restore()

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BAR)
    }

    @Test
    fun `enabling floating keeps a bubble that is already shown`() {
        CallBarPresenter.minimizeToBubble()

        CallBarPresenter.setFloatingEnabled(true)

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BUBBLE)
    }

    @Test
    fun `restoring a hidden bubble with floating enabled brings the bubble back`() {
        CallBarPresenter.minimizeToBubble()
        CallBarPresenter.hide()

        CallBarPresenter.restore()

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BUBBLE)
    }

    @Test
    fun `disabling floating while the video tile is shown falls back to the bar`() {
        CallBarPresenter.showVideoTile()

        CallBarPresenter.setFloatingEnabled(false)

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BAR)
    }

    @Test
    fun `restoring a hidden video tile with floating disabled shows the bar`() {
        CallBarPresenter.showVideoTile()
        CallBarPresenter.hide()
        CallBarPresenter.setFloatingEnabled(false)

        CallBarPresenter.restore()

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.BAR)
    }

    @Test
    fun `a dismissed video tile does not come back on its own`() {
        CallBarPresenter.showVideoTile()
        CallBarPresenter.hide()

        CallBarPresenter.showVideoTile()

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.HIDDEN)
    }

    @Test
    fun `restoring a dismissed video tile lets it show again later`() {
        CallBarPresenter.showVideoTile()
        CallBarPresenter.hide()
        CallBarPresenter.restore()
        CallBarPresenter.collapseVideoTile()

        CallBarPresenter.showVideoTile()

        assertThat(CallBarPresenter.mode.value).isEqualTo(CallBarMode.VIDEO)
    }
}
