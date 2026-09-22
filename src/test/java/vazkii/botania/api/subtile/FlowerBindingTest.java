package vazkii.botania.api.subtile;

import static org.junit.Assert.*;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.Test;

import vazkii.botania.common.block.tile.mana.TilePool;
import vazkii.botania.common.block.tile.mana.TileSpreader;

public class FlowerBindingTest {

	private static final int[] HEIGHTS = { Integer.MIN_VALUE, -512, -32, -1, 0, 64, 255, 512, Integer.MAX_VALUE };

	private static TileEntity target(int y) {
		TileEntity tile = new TileEntity();
		tile.xCoord = 0;
		tile.yCoord = y;
		tile.zCoord = 0;
		return tile;
	}

	private static NBTTagCompound coordinates(String prefix, int y) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger(prefix + "X", 0);
		nbt.setInteger(prefix + "Y", y);
		nbt.setInteger(prefix + "Z", 0);
		return nbt;
	}

	private static void assertPosition(ChunkCoordinates position, int y) {
		assertNotNull(position);
		assertEquals(0, position.posX);
		assertEquals(y, position.posY);
		assertEquals(0, position.posZ);
	}

	@Test
	public void functionalBindingsRoundTripAtEveryHeight() {
		for(int y : HEIGHTS) {
			SubTileFunctional flower = new SubTileFunctional();
			flower.linkToForcefully(target(y));
			flower.mana = 17;
			flower.ticksExisted = 20;
			NBTTagCompound nbt = new NBTTagCompound();
			flower.writeToPacketNBTInternal(nbt);
			assertTrue(nbt.getBoolean("hasPool"));
			SubTileFunctional loaded = new SubTileFunctional();
			loaded.readFromPacketNBTInternal(nbt);
			assertPosition(loaded.cachedPoolCoordinates, y);
			assertEquals(17, loaded.mana);
			assertEquals(20, loaded.ticksExisted);
			NBTTagCompound pendingSave = new NBTTagCompound();
			loaded.writeToPacketNBTInternal(pendingSave);
			assertEquals(nbt, pendingSave);
		}
	}

	@Test
	public void generatingBindingsRoundTripAtEveryHeight() {
		for(int y : HEIGHTS) {
			SubTileGenerating flower = new SubTileGenerating();
			flower.linkToForcefully(target(y));
			flower.mana = 17;
			flower.ticksExisted = 20;
			flower.passiveDecayTicks = 23;
			NBTTagCompound nbt = new NBTTagCompound();
			flower.writeToPacketNBTInternal(nbt);
			assertTrue(nbt.getBoolean("hasCollector"));
			SubTileGenerating loaded = new SubTileGenerating();
			loaded.readFromPacketNBTInternal(nbt);
			assertPosition(loaded.cachedCollectorCoordinates, y);
			assertEquals(17, loaded.mana);
			assertEquals(20, loaded.ticksExisted);
			assertEquals(23, loaded.passiveDecayTicks);
			NBTTagCompound pendingSave = new NBTTagCompound();
			loaded.writeToPacketNBTInternal(pendingSave);
			assertEquals(nbt, pendingSave);
		}
	}

	@Test
	public void oldSavesRetainLegacyBindingInterpretation() {
		for(int y : HEIGHTS) {
			SubTileFunctional functional = new SubTileFunctional();
			functional.readFromPacketNBT(coordinates("pool", y));
			SubTileGenerating generating = new SubTileGenerating();
			generating.readFromPacketNBT(coordinates("collector", y));
			if(y < 0) {
				assertNull(functional.cachedPoolCoordinates);
				assertNull(generating.cachedCollectorCoordinates);
			} else {
				assertPosition(functional.cachedPoolCoordinates, y);
				assertPosition(generating.cachedCollectorCoordinates, y);
			}
		}
	}

	@Test
	public void packetsReplaceExistingBindingsAndCanUnbind() {
		SubTileFunctional functional = new SubTileFunctional();
		functional.linkToForcefully(target(64));
		NBTTagCompound pool = coordinates("pool", -32);
		pool.setBoolean("hasPool", true);
		functional.readFromPacketNBT(pool);
		assertNull(functional.linkedPool);
		assertPosition(functional.cachedPoolCoordinates, -32);
		functional.linkToForcefully(target(64));
		pool.setInteger("poolY", 64);
		pool.setBoolean("hasPool", false);
		functional.readFromPacketNBT(pool);
		assertNull(functional.linkedPool);
		assertNull(functional.cachedPoolCoordinates);

		SubTileGenerating generating = new SubTileGenerating();
		generating.linkToForcefully(target(64));
		NBTTagCompound collector = coordinates("collector", -32);
		collector.setBoolean("hasCollector", true);
		generating.readFromPacketNBT(collector);
		assertNull(generating.linkedCollector);
		assertPosition(generating.cachedCollectorCoordinates, -32);
		generating.linkToForcefully(target(64));
		collector.setInteger("collectorY", 64);
		collector.setBoolean("hasCollector", false);
		generating.readFromPacketNBT(collector);
		assertNull(generating.linkedCollector);
		assertNull(generating.cachedCollectorCoordinates);
	}

	@Test
	public void unboundFlowersStayUnboundThroughSaveAndReload() {
		NBTTagCompound pool = new NBTTagCompound();
		new SubTileFunctional().writeToPacketNBT(pool);
		assertTrue(pool.hasKey("hasPool"));
		assertFalse(pool.getBoolean("hasPool"));
		assertEquals(-1, pool.getInteger("poolY"));
		SubTileFunctional functional = new SubTileFunctional();
		functional.readFromPacketNBT(pool);
		assertNull(functional.cachedPoolCoordinates);

		NBTTagCompound collector = new NBTTagCompound();
		new SubTileGenerating().writeToPacketNBT(collector);
		assertTrue(collector.hasKey("hasCollector"));
		assertFalse(collector.getBoolean("hasCollector"));
		assertEquals(-1, collector.getInteger("collectorY"));
		SubTileGenerating generating = new SubTileGenerating();
		generating.readFromPacketNBT(collector);
		assertNull(generating.cachedCollectorCoordinates);
	}

	@Test
	public void manualBindingAndUnbindingReplacePendingCoordinates() {
		SubTileFunctional functional = new SubTileFunctional();
		functional.cachedPoolCoordinates = new ChunkCoordinates(0, 64, 0);
		functional.linkToForcefully(target(-1));
		assertNull(functional.cachedPoolCoordinates);
		NBTTagCompound pool = new NBTTagCompound();
		functional.writeToPacketNBT(pool);
		assertTrue(pool.getBoolean("hasPool"));
		assertEquals(-1, pool.getInteger("poolY"));
		functional.linkToForcefully(null);
		functional.writeToPacketNBT(pool);
		assertFalse(pool.getBoolean("hasPool"));
		assertEquals(-1, pool.getInteger("poolY"));

		SubTileGenerating generating = new SubTileGenerating();
		generating.cachedCollectorCoordinates = new ChunkCoordinates(0, 64, 0);
		generating.linkToForcefully(target(-1));
		assertNull(generating.cachedCollectorCoordinates);
		NBTTagCompound collector = new NBTTagCompound();
		generating.writeToPacketNBT(collector);
		assertTrue(collector.getBoolean("hasCollector"));
		assertEquals(-1, collector.getInteger("collectorY"));
		generating.linkToForcefully(null);
		generating.writeToPacketNBT(collector);
		assertFalse(collector.getBoolean("hasCollector"));
		assertEquals(-1, collector.getInteger("collectorY"));
	}

	@Test
	public void functionalBindingWaitsForTheTargetCube() {
		for(int y : HEIGHTS) {
			BindingWorld world = new BindingWorld(y);
			SubTileFunctional flower = new SubTileFunctional();
			TileEntity container = target(y);
			container.setWorldObj(world);
			flower.setSupertile(container);
			flower.linkToForcefully(target(64));
			NBTTagCompound nbt = coordinates("pool", y);
			nbt.setBoolean("hasPool", true);
			flower.readFromPacketNBT(nbt);
			flower.linkPool();
			assertPosition(flower.cachedPoolCoordinates, y);
			assertNull(flower.linkedPool);
			assertEquals(0, world.tileLookups);

			NBTTagCompound pendingSave = new NBTTagCompound();
			flower.writeToPacketNBT(pendingSave);
			assertTrue(pendingSave.getBoolean("hasPool"));
			assertEquals(y, pendingSave.getInteger("poolY"));

			world.loaded = true;
			world.targetTile = new TilePool();
			world.targetTile.yCoord = y;
			flower.linkPool();
			assertSame(world.targetTile, flower.linkedPool);
			assertNull(flower.cachedPoolCoordinates);
			assertEquals(1, world.tileLookups);

			flower.readFromPacketNBT(nbt);
			world.targetTile = null;
			flower.linkPool();
			assertNull(flower.cachedPoolCoordinates);
			assertNull(flower.linkedPool);
		}
	}

	@Test
	public void generatingBindingWaitsForTheTargetCube() {
		for(int y : HEIGHTS) {
			BindingWorld world = new BindingWorld(y);
			SubTileGenerating flower = new SubTileGenerating();
			TileEntity container = target(y);
			container.setWorldObj(world);
			flower.setSupertile(container);
			flower.linkToForcefully(target(64));
			NBTTagCompound nbt = coordinates("collector", y);
			nbt.setBoolean("hasCollector", true);
			flower.readFromPacketNBT(nbt);
			flower.linkCollector();
			assertPosition(flower.cachedCollectorCoordinates, y);
			assertNull(flower.linkedCollector);
			assertEquals(0, world.tileLookups);

			NBTTagCompound pendingSave = new NBTTagCompound();
			flower.writeToPacketNBT(pendingSave);
			assertTrue(pendingSave.getBoolean("hasCollector"));
			assertEquals(y, pendingSave.getInteger("collectorY"));

			world.loaded = true;
			world.targetTile = new TileSpreader();
			world.targetTile.yCoord = y;
			flower.linkCollector();
			assertSame(world.targetTile, flower.linkedCollector);
			assertNull(flower.cachedCollectorCoordinates);
			assertEquals(1, world.tileLookups);

			flower.readFromPacketNBT(nbt);
			world.targetTile = null;
			flower.linkCollector();
			assertNull(flower.cachedCollectorCoordinates);
			assertNull(flower.linkedCollector);
		}
	}

	/** Models a saved cube which may not have loaded yet, without booting Minecraft. */
	private static class BindingWorld extends World {
		private final int targetY;
		boolean loaded;
		TileEntity targetTile;
		int tileLookups;

		BindingWorld(int targetY) {
			super(null, "binding-test", new WorldProviderSurface(),
					new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT), new Profiler());
			this.targetY = targetY;
		}

		@Override
		public boolean blockExists(int x, int y, int z) {
			return true;
		}

		@Override
		public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) {
			assertEquals(0, x1);
			assertEquals(targetY, y1);
			assertEquals(0, z1);
			assertEquals(x1, x2);
			assertEquals(y1, y2);
			assertEquals(z1, z2);
			return loaded;
		}

		@Override
		public TileEntity getTileEntity(int x, int y, int z) {
			tileLookups++;
			return targetTile;
		}

		@Override
		protected IChunkProvider createChunkProvider() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected int func_152379_p() {
			return 0;
		}

		@Override
		public Entity getEntityByID(int id) {
			return null;
		}
	}
}
