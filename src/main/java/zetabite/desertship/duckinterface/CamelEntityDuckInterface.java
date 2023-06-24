package zetabite.desertship.duckinterface;

public interface CamelEntityDuckInterface {
	boolean hasChest();
	void setHasChest(boolean hasChest);
	int getInventorySlotCount();
	int getInventoryColumns();
	int getInventoryRows();
	void applyChestedChange(boolean hasChest);
}
