/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.rlcommunity.btanner.agentLib.functionApproximators.CMAC;

import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * Generic tile coder over n-dimensional continuous observations.
 * NOT THREAD SAFE
 * @author Brian Tanner
 */
public class NDCMACTileCoder implements FeatureBasedFunctionApproximatorInterface {

    private final double[] theWeights;
    private final double theGridSize;
    private final TileCoder theTC;
    private final int numTilings;
    private final int[] activeTiles;

    public static void addToParameterHolder(ParameterHolder p) {
        p.addIntegerParam("cmac-grid-size", 8);
        p.addIntegerParam("cmac-num-tilings", 16);
        p.addIntegerParam("cmac-memory-size", 50000);
        p.addDoubleParam("cmac-initial-value", 0.0d);

        p.setAlias("cmac-memorySize", "cmac-memory-size");
        p.setAlias("cmac-numTilings", "cmac-num-tilings");
        p.setAlias("cmac-gridSize", "cmac-grid-size");
    }

    public NDCMACTileCoder(ParameterHolder p) {
        int memorySize = p.getIntegerParam("cmac-memory-size");
        this.theGridSize = p.getIntegerParam("cmac-grid-size");
        this.numTilings = p.getIntegerParam("cmac-num-tilings");

        double initialValue = p.getDoubleParam("cmac-initial-value");

        theWeights = new double[memorySize];
        for (int i = 0; i < theWeights.length; i++) {
            //Split the initial weight over all tiles
            theWeights[i] = initialValue / (double) this.numTilings;
        }

        theTC = new TileCoder();
        activeTiles = new int[numTilings];

    }

    public void init() {
    }

    /**
     *
     * @param normalizedObservation
     * @param delta We assume that delta has been reduced by a step size already,
     * but has not been split to consider how many tiles there are (because the
     * tile coder is black box that way).  Don't worry, we will scale delta.
     */
    public final void update(Observation normalizedObservation, double delta) {
        fillTiles(normalizedObservation, -1);

        double splitWeight = delta / (double) activeTiles.length;
        for (int thisTile : activeTiles) {
            theWeights[thisTile] += splitWeight;
        }
    }

    private final void fillTiles(Observation normalizedObservation, long uniqueId) {
        fillTiles(activeTiles, normalizedObservation, uniqueId);
    }

    private final void fillTiles(int[] tilesToFill, Observation normalizedObservation, long uniqueId) {
        //We have our own parameter space.
        final int tileStartOffset = 0;
        int memorySize = theWeights.length;
        final int[] noInts = new int[0];

        //Would be nice not to recreate this every time filltiles gets called.
        double[] scaledDoubles = new double[normalizedObservation.doubleArray.length];
        for (int i = 0; i < scaledDoubles.length; i++) {
            scaledDoubles[i] = normalizedObservation.doubleArray[i] * theGridSize;
        }

        theTC.tiles(tilesToFill, tileStartOffset, numTilings, memorySize, scaledDoubles, noInts);

    }

    public double query(Observation normalizedObservation) {
        return query(normalizedObservation, -1);
    }

    public double query(Observation normalizedObservation, long uniqueId) {
        //Check if uniqueId is valid or not
        double valueEstimate = 0.0d;

        fillTiles(normalizedObservation, uniqueId);
        for (int thisTile : activeTiles) {
            valueEstimate += theWeights[thisTile];
        }

        return valueEstimate;

    }

    /*
    The next few functions are because we implement FeatureBasedFunctionApproximatorInterface
     */
    public int getMemorySize() {
        return theWeights.length;
    }

    public int getActiveFeatureCount() {
        return numTilings;
    }

    public void setActiveFeatures(Observation normalizedObservation, int[] theFeatures) {
        fillTiles(theFeatures, normalizedObservation, -1);
    }

    public void incrementFeatureByAmount(int whatFeature, double delta) {
        double splitWeight = delta / (double) activeTiles.length;
        theWeights[whatFeature] += splitWeight;
    }

    public void cleanup() {
    }
}
