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

import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Objects;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin<T extends AbstractContainerMenu> extends EffectRenderingInventoryScreen<T> implements FabricCreativeInventoryScreen {
	@Shadow
	@Final
	private List<CreativeTabsScreenPage> pages;
	@Shadow(aliases = "currentPage")
	private CreativeTabsScreenPage currentSelectedPage;
	@Shadow
	private static CreativeModeTab selectedTab;

	@Unique // Kept for mod internal usage
	private static int currentPage;

	@Shadow
	abstract void setCurrentPage(CreativeTabsScreenPage currentPage);

	@Shadow
	abstract void selectTab(CreativeModeTab arg);
	
	public CreativeInventoryScreenMixin(T screenHandler, Inventory playerInventory, Component text) {
		super(screenHandler, playerInventory, text);
	}

	@Override
	public boolean switchToPage(int page) {
		if (pages.size() < page) {
			return false;
		}
		setCurrentPage(pages.get(page));
		return true; 
	}

	@Override
	public boolean switchToNextPage() {
		if (hasAdditionalPages()) {
			this.setCurrentPage(this.pages.get(this.pages.indexOf(this.currentSelectedPage) + 1));
			return true;
		}
		return false;
	}

	@Override
	public boolean switchToPreviousPage() {
		if (this.pages.indexOf(this.currentSelectedPage) > 0) {
			this.setCurrentPage(this.pages.get(this.pages.indexOf(this.currentSelectedPage) - 1));
			return true;
		}
		return false;
	}

	@Override
	public int getCurrentPage() {
		return this.pages.indexOf(this.currentSelectedPage);
	}

	@Override
	public int getPageCount() {
		return this.pages.size();
	}

	@Override
	public List<CreativeModeTab> getItemGroupsOnPage(int page) {
		return this.pages.get(page).getVisibleTabs();
	}

	@Override
	public int getPage(CreativeModeTab itemGroup) {
		return this.pages.stream().filter(p -> p.getVisibleTabs().contains(itemGroup)).findFirst()
			.map(this.pages::indexOf)
			.orElse(-1);
	}

	@Override
	public boolean hasAdditionalPages() {
		return this.pages.indexOf(this.currentSelectedPage) < this.pages.size() - 1;
	}

	@Override
	public CreativeModeTab getSelectedItemGroup() {
		return selectedTab;
	}

	@Override
	public boolean setSelectedItemGroup(CreativeModeTab itemGroup) {
		Objects.requireNonNull(itemGroup, "itemGroup");

		if (selectedTab == itemGroup) {
			return false;
		}

		if (pages.indexOf(currentSelectedPage) != getPage(itemGroup)) {
			if (!switchToPage(getPage(itemGroup))) {
				return false;
			}
		}

		selectTab(itemGroup);
		return true;
	}

	@Unique
	@Deprecated // Kept for mod internals
	private boolean isGroupVisible(CreativeModeTab tab) {
		return tab.shouldDisplay() && getCurrentPage() == getPage(tab);
	}

	@Unique
	@Deprecated // Kept for mod internals
	private void updateSelection() {}
}
