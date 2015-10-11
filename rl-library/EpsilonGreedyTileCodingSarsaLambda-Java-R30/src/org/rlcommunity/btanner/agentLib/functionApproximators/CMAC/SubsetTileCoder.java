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

import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * Not meant to be exposed to the outside, this class is meant to be used
 * as a compent of a larger tile coding system.  This class takes a subset
 * of variables and tile codes over them.
 * NOT THREAD SAFE
 * @author Brian Tanner
 */
class SubsetTileCoder{

    private final double[] theWeights;
    private final double theGridSize;
    private final TileCoder theTC;
    private final int numTilings;
    private final int[] activeTiles;
    private final int[] whichDimensions;

    public SubsetTileCoder(int memorySize, int gridSize, int numTilings, double initialValue,int[] whichDimensions) {
        this.whichDimensions=whichDimensions;
        this.theGridSize=gridSize;
        this.numTilings=numTilings;

        theWeights = new double[memorySize];
        for (int i = 0; i < theWeights.length; i++) {
            //Split the initial weight over all tiles
            theWeights[i] = initialValue / (double) this.numTilings;
        }

        theTC = new TileCoder();
        activeTiles = new int[numTilings];
    }

    public int getNumDimensions(){
        return whichDimensions.length;
    }

    /**
     *
     * @param normalizedObservation
     * @param delta We assume that delta has not been reduced by a step size already,
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
        fillTiles(activeTiles, normalizedObservation, uniqueId,0);
    }

    /**
     * This is the workhorse method.  It will pick out the dimensions it needs.
     * @param tilesToFill
     * @param normalizedObservation
     * @param uniqueId
     */
    private final void fillTiles(int[] tilesToFill, Observation normalizedObservation, long uniqueId, int offset) {
        int memorySize = theWeights.length;
        final int[] noInts = new int[0];

        //Would be nice not to recreate this every time filltiles gets called.
        double[] scaledDoubles = new double[whichDimensions.length];
        for (int i = 0; i < whichDimensions.length; i++) {
            scaledDoubles[i] = normalizedObservation.doubleArray[whichDimensions[i]] * theGridSize;
        }

        theTC.tiles(tilesToFill, offset, numTilings, memorySize, scaledDoubles, noInts);

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
        fillTiles(theFeatures, normalizedObservation, -1,0);
    }
    void setActiveFeatures(Observation normalizedObservation, int[] theFeatures, int offset) {
        fillTiles(theFeatures, normalizedObservation, -1L,offset);
    }

    /**
     * 
     * @param whatFeature
     * @param delta Do not scale delta by the number of features in this tilecoder
     */
    public void incrementFeatureByAmount(int whatFeature, double delta) {
        double splitWeight = delta / (double) activeTiles.length;
        theWeights[whatFeature] += splitWeight;
    }

}
