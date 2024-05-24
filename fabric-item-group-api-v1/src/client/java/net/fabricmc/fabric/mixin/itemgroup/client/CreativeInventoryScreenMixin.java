/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.itemgroup.client;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin<T extends AbstractContainerMenu> extends EffectRenderingInventoryScreen<T> implements CreativeGuiExtensions {
	public CreativeInventoryScreenMixin(T screenHandler, Inventory playerInventory, Component text) {
		super(screenHandler, playerInventory, text);
	}

	@Shadow
	protected abstract void selectTab(CreativeModeTab itemGroup_1);

	@Shadow
	private static CreativeModeTab selectedTab;

	// "static" matches selectedTab
	private static int fabric_currentPage = 0;

	@Override
	public void fabric_nextPage() {
		if (!fabric_hasGroupForPage(fabric_currentPage + 1)) {
			return;
		}

		fabric_currentPage++;
		fabric_updateSelection();
	}

	@Override
	public void fabric_previousPage() {
		if (fabric_currentPage == 0) {
			return;
		}

		fabric_currentPage--;
		fabric_updateSelection();
	}

	@Override
	public boolean fabric_isButtonVisible(FabricCreativeGuiComponents.Type type) {
		return CreativeModeTabs.tabs().size() > (Objects.requireNonNull(CreativeModeTabs.CACHED_PARAMETERS).hasPermissions() ? 14 : 13);
	}

	@Override
	public boolean fabric_isButtonEnabled(FabricCreativeGuiComponents.Type type) {
		if (type == FabricCreativeGuiComponents.Type.NEXT) {
			return fabric_hasGroupForPage(fabric_currentPage + 1);
		}

		if (type == FabricCreativeGuiComponents.Type.PREVIOUS) {
			return fabric_currentPage != 0;
		}

		return false;
	}

	private void fabric_updateSelection() {
		if (!fabric_isGroupVisible(selectedTab)) {
			CreativeModeTabs.allTabs()
					.stream()
					.filter(this::fabric_isGroupVisible)
					.min((a, b) -> Boolean.compare(a.isAlignedRight(), b.isAlignedRight()))
					.ifPresent(this::selectTab);
		}
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo info) {
		fabric_currentPage = fabric_getPage(selectedTab);

		int xpos = leftPos + 170;
		int ypos = topPos + 4;

		addRenderableWidget(new FabricCreativeGuiComponents.ItemGroupButtonWidget(xpos + 11, ypos, FabricCreativeGuiComponents.Type.NEXT, this));
		addRenderableWidget(new FabricCreativeGuiComponents.ItemGroupButtonWidget(xpos, ypos, FabricCreativeGuiComponents.Type.PREVIOUS, this));
	}

	@Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
	private void setSelectedTab(CreativeModeTab itemGroup, CallbackInfo info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.cancel();
		}
	}

	@Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
	private void renderTabTooltipIfHovered(GuiGraphics drawContext, CreativeModeTab itemGroup, int mx, int my, CallbackInfoReturnable<Boolean> info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "checkTabClicked", at = @At("HEAD"), cancellable = true)
	private void isClickInTab(CreativeModeTab itemGroup, double mx, double my, CallbackInfoReturnable<Boolean> info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true)
	private void renderTabIcon(GuiGraphics drawContext, CreativeModeTab itemGroup, CallbackInfo info) {
		if (!fabric_isGroupVisible(itemGroup)) {
			info.cancel();
		}
	}

	private boolean fabric_isGroupVisible(CreativeModeTab itemGroup) {
		return itemGroup.shouldDisplay() && fabric_currentPage == fabric_getPage(itemGroup);
	}

	private static int fabric_getPage(CreativeModeTab itemGroup) {
		if (FabricCreativeGuiComponents.COMMON_GROUPS.contains(itemGroup)) {
			return fabric_currentPage;
		}

		final FabricItemGroup fabricItemGroup = (FabricItemGroup) itemGroup;
		return fabricItemGroup.getPage();
	}

	private static boolean fabric_hasGroupForPage(int page) {
		return CreativeModeTabs.tabs().stream()
				.anyMatch(itemGroup -> fabric_getPage(itemGroup) == page);
	}

	@Override
	public int fabric_currentPage() {
		return fabric_currentPage;
	}
}
