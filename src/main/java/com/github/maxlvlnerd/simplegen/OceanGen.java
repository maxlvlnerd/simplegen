package com.github.maxlvlnerd.simplegen;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.generator.UnitModifier;
import org.jetbrains.annotations.NotNull;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class OceanGen implements Generator {
    private static final Block top_seagrass = Block.TALL_SEAGRASS.withProperty("half", "upper");
    private final FastNoiseLite noise;
    private final FastNoiseLite sandNoise;
    private final FastNoiseLite kelpNoise;
    private final int seed;


    public OceanGen(int seed) {
        this.noise = new FastNoiseLite(seed);
        this.kelpNoise = new FastNoiseLite(seed * 31);
        this.sandNoise = new FastNoiseLite((seed * 31) * 31);
        this.seed = seed;
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        noise.SetFrequency(0.006f);
        noise.SetFractalType(FastNoiseLite.FractalType.FBm);
        noise.SetFractalOctaves(5);
        noise.SetDomainWarpType(FastNoiseLite.DomainWarpType.OpenSimplex2);
        noise.SetDomainWarpAmp(100f);
        kelpNoise.SetNoiseType(FastNoiseLite.NoiseType.Value);
        kelpNoise.SetFrequency(0.05f);
        kelpNoise.SetDomainWarpType(FastNoiseLite.DomainWarpType.OpenSimplex2Reduced);
        kelpNoise.SetDomainWarpAmp(100f);
        sandNoise.SetNoiseType(FastNoiseLite.NoiseType.ValueCubic);
        sandNoise.SetFrequency(0.01f);
        sandNoise.SetDomainWarpType(FastNoiseLite.DomainWarpType.BasicGrid);
        sandNoise.SetDomainWarpAmp(500f);
    }


    public void terrain(UnitModifier modifier, float[] heightMap, Point start) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var height = heightMap[(x * 16) + z];
                modifier.fill(start.add(x, 0, z), start.add(x + 1, height, z + 1), Block.STONE);
                modifier.setBlock(start.add(x, height - 1, z), getTopBlock(height, start.add(x, 0, z)));
            }
        }
    }

    public void decorate(UnitModifier modifier, float[] heightMap, Point start, RandomGenerator rng) {
        for (int x1 = 0; x1 < 16; x1++) {
            for (int z1 = 0; z1 < 16; z1++) {
                var height = heightMap[(x1 * 16) + z1];
                var kelp = new FastNoiseLite.Vector2((float) (start.x() + x1), (float) (start.z() + z1));
                var seagrass = new FastNoiseLite.Vector2((float) (start.x() + x1), (float) (start.z() + z1));
                this.kelpNoise.DomainWarp(seagrass);
                this.kelpNoise.DomainWarp(seagrass);
                this.kelpNoise.DomainWarp(kelp);
                // looks better imo when low and high parts don't have kelp
                if (height > 22 && height <= 35 && rng.nextFloat() > 0.7f && kelpNoise.GetNoise(kelp.x, kelp.y) > 0.3f) {
                    modifier.fill(start.add(x1, height, z1), start.add(x1 + 1, Math.min(height + 10 + rng.nextInt(5), 45), z1 + 1), Block.KELP_PLANT);
                } else if (rng.nextFloat() > 0.8f && kelpNoise.GetNoise(seagrass.x, seagrass.y) > 0f) {
                    if (rng.nextBoolean()) {
                        modifier.setBlock(start.add(x1, height, z1), Block.TALL_SEAGRASS);
                        modifier.setBlock(start.add(x1, height + 1, z1), top_seagrass);
                    } else {
                        modifier.setBlock(start.add(x1, height, z1), Block.SEAGRASS);
                    }
                }
            }
        }
    }

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        var modifier = unit.modifier();
        var start = unit.absoluteStart().withY(0.0);
        var heightMap = genHeightmap(start);
        modifier.fillHeight(0, 50, Block.WATER);
        terrain(modifier, heightMap, start);
        decorate(modifier, heightMap, start, newRng(chunkSeed(this.seed, start.blockX(), start.blockZ())));
    }

    private Block getTopBlock(float height, Point point) {
        var weights = new Spline(new float[]{10f, 25f, 40f, 45f}, new float[]{0.1f, 0.1f, 1.5f, 1.5f});
        //noinspection SuspiciousNameCombination
        var weight = weights.interp(OceanGen::smoothstep, height);
        var pos = new FastNoiseLite.Vector2((float) point.x(), (float) point.z());
        this.sandNoise.DomainWarp(pos);
        var nv = (this.sandNoise.GetNoise(pos.x, pos.y) + 1f) / 2f;
        var keys = new float[]{0f, 0.6f, 0.7f, 0.8f, 1f};
        var values = new Block[]{Block.SAND, Block.GRAVEL, Block.DIRT, Block.SAND, Block.SAND};
        return values[Spline.last_without_going_over(keys, nv * weight)];
    }


    public float[] genHeightmap(Point pos) {
        var spline = new Spline(new float[]{-1f, -0.6f, 0f, 0.8f, 1f}, new float[]{10, 13, 25, 40, 45});
        var height = new float[16 * 16];
        for (int x1 = 0; x1 < 16; x1++) {
            for (int z1 = 0; z1 < 16; z1++) {
                var thing = new FastNoiseLite.Vector2((float) (pos.x() + x1), (float) (pos.z() + z1));
                this.noise.DomainWarp(thing);
                var n = this.noise.GetNoise(thing.x, thing.y);
                height[(x1 * 16) + z1] = spline.interp(OceanGen::smoothstep, n);
            }
        }

        return height;
    }

    // just a way to generate a rng seed for a chunk
    private long chunkSeed(long seed, int x, int z) {
        return seed * x * z;
    }

    private RandomGenerator newRng(long seed) {
        return RandomGeneratorFactory.getDefault().create(seed);
    }

    // Perlin variant from https://en.wikipedia.org/wiki/Smoothstep
    public static float smoothstep(float x) {
        return (float) (6 * Math.pow(x, 5) - 15 * Math.pow(x, 4) + 10 * Math.pow(x, 3));
    }
}
