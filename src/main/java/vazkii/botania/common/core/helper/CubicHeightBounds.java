package vazkii.botania.common.core.helper;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.world.ICubicWorld;
import com.cardinalstar.cubicchunks.world.cube.ICubeProvider;

import cpw.mods.fml.common.Loader;

public final class CubicHeightBounds {

	private CubicHeightBounds() {}

	public static boolean contains(World world, long minY, long maxY, boolean vanillaResult) {
		if(minY < Integer.MIN_VALUE || maxY > Integer.MAX_VALUE || minY > maxY)
			return false;
		if(Loader.isModLoaded("cubicchunks"))
			return CubicWorldBounds.contains(world, (int) minY, (int) maxY, vanillaResult);
		return vanillaResult;
	}

	private static final class CubicWorldBounds {

		private static boolean contains(World world, int minY, int maxY, boolean vanillaResult) {
			if(world.getChunkProvider() instanceof ICubeProvider) {
				ICubicWorld cubicWorld = (ICubicWorld) world;
				return minY >= cubicWorld.getMinHeight() && maxY < cubicWorld.getMaxHeight();
			}
			return vanillaResult;
		}
	}
}
