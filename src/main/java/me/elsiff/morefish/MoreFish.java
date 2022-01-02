package me.elsiff.morefish;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.Lists;
import me.elsiff.egui.GuiOpener;
import me.elsiff.egui.GuiRegistry;
import me.elsiff.morefish.command.MainCommand;
import me.elsiff.morefish.configuration.Config;
import me.elsiff.morefish.dao.DaoFactory;
import me.elsiff.morefish.fishing.FishingListener;
import me.elsiff.morefish.fishing.MutableFishTypeTable;
import me.elsiff.morefish.fishing.catchhandler.CatchBroadcaster;
import me.elsiff.morefish.fishing.catchhandler.CatchHandler;
import me.elsiff.morefish.fishing.catchhandler.CompetitionRecordAdder;
import me.elsiff.morefish.fishing.catchhandler.NewFirstBroadcaster;
import me.elsiff.morefish.fishing.competition.FishingCompetition;
import me.elsiff.morefish.fishing.competition.FishingCompetitionAutoRunner;
import me.elsiff.morefish.fishing.competition.FishingCompetitionHost;
import me.elsiff.morefish.hooker.CitizensHooker;
import me.elsiff.morefish.hooker.McmmoHooker;
import me.elsiff.morefish.hooker.PlaceholderApiHooker;
import me.elsiff.morefish.hooker.PluginHooker;
import me.elsiff.morefish.hooker.ProtocolLibHooker;
import me.elsiff.morefish.hooker.VaultHooker;
import me.elsiff.morefish.hooker.WorldGuardHooker;
import me.elsiff.morefish.item.FishItemStackConverter;
import me.elsiff.morefish.shop.FishShop;
import me.elsiff.morefish.shop.FishShopSignListener;
import me.elsiff.morefish.update.UpdateChecker;
import me.elsiff.morefish.util.OneTickScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MoreFish extends JavaPlugin {

	private static MoreFish instance;

	private final ProtocolLibHooker protocolLib = new ProtocolLibHooker();
	private final VaultHooker vault = new VaultHooker();
	private final McmmoHooker mcMmoHooker = new McmmoHooker();
	private final WorldGuardHooker worldGuardHooker = new WorldGuardHooker();
	private final CitizensHooker citizensHooker = new CitizensHooker();
	private final PlaceholderApiHooker placeholderApiHooker = new PlaceholderApiHooker();

	private final GuiRegistry guiRegistry = new GuiRegistry(this);
	private final GuiOpener guiOpener = new GuiOpener(guiRegistry);
	private final OneTickScheduler oneTickScheduler = new OneTickScheduler(this);

	private final MutableFishTypeTable fishTypeTable = new MutableFishTypeTable();
	private final FishingCompetition competition = new FishingCompetition();
	private final FishingCompetitionHost competitionHost = new FishingCompetitionHost(this, competition);
	private final FishingCompetitionAutoRunner autoRunner = new FishingCompetitionAutoRunner(this, competitionHost);
	private final FishItemStackConverter converter = new FishItemStackConverter(this, fishTypeTable);
	private final FishShop fishShop = new FishShop(guiOpener, oneTickScheduler, converter, vault);
	private final List<CatchHandler> globalCatchHandlers = Lists.newArrayList(new CatchBroadcaster(), new NewFirstBroadcaster(competition), new CompetitionRecordAdder(competition));
	private final UpdateChecker updateChecker = new UpdateChecker(22926, getDescription().getVersion());

	@Override
	public void onEnable() {
		instance = this;
		DaoFactory.init(this);

		for (PluginHooker pluginHooker : Lists.newArrayList(protocolLib, vault, mcMmoHooker, worldGuardHooker, citizensHooker, placeholderApiHooker)) {
			pluginHooker.hookIfEnabled(this);
		}

		applyConfig();

		getServer().getPluginManager().registerEvents(new FishingListener(fishTypeTable, converter, competition, globalCatchHandlers), this);
		getServer().getPluginManager().registerEvents(new FishShopSignListener(fishShop), this);

		final PaperCommandManager commands = new PaperCommandManager(this);
		commands.registerCommand(new MainCommand(this, competitionHost, fishShop));

		if (!isSnapshotVersion() && Config.INSTANCE.getStandard().boolean("general.check-updates")) {

		}
	}

	private boolean isSnapshotVersion() {
		return getDescription().getVersion().contains("SNAPSHOT");
	}

}