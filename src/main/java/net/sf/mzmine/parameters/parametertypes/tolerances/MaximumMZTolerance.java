/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes.tolerances;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;

import com.google.common.collect.Range;

/**
 * This class represents m/z tolerance. Tolerance is set using absolute (m/z)
 * and relative (ppm) values. The tolerance range is calculated as the maximum
 * of the absolute and relative values.
 */
public class MaximumMZTolerance extends MZTolerance {

    // PPM conversion factor.
    private static final double MILLION = 1000000.0;

    // Tolerance has absolute (in m/z) and relative (in ppm) values
	private static final DoubleParameter mzTolerance = new DoubleParameter(
			"mzTolerance", "The absolute (in m/z) tolerance");
	private static final DoubleParameter ppmTolerance = new DoubleParameter(
			"ppmTolerance", "The relative (in ppm) tolerance");

    public MaximumMZTolerance() {
    	super(new Parameter[] { mzTolerance, ppmTolerance});
    }

    public double getMzTolerance() {
	return mzTolerance.getValue();
    }

    public double getPpmTolerance() {
	return ppmTolerance.getValue();
    }

    private double getMzToleranceForMass(final double mzValue) {
	return Math.max(mzTolerance.getValue(), mzValue / MILLION * ppmTolerance.getValue());
    }

    public double getPpmToleranceForMass(final double mzValue) {
	return Math.max(ppmTolerance.getValue(), mzTolerance.getValue() / (mzValue / MILLION));
    }

    @Override
    public Range<Double> getToleranceRange(final double mzValue) {
	final double absoluteTolerance = getMzToleranceForMass(mzValue);
	return Range.closed(mzValue - absoluteTolerance, mzValue
		+ absoluteTolerance);
    }

    @Override
    public Range<Double> getToleranceRange(final Range<Double> mzRange) {
	return Range.closed(
		mzRange.lowerEndpoint()
			- getMzToleranceForMass(mzRange.lowerEndpoint()),
		mzRange.upperEndpoint()
			+ getMzToleranceForMass(mzRange.upperEndpoint()));
    }

    @Override
    public boolean checkWithinTolerance(final double mz1, final double mz2) {
	return getToleranceRange(mz1).contains(mz2);
    }

    @Override
    public String toString() {
		return "Maximum of m/z or ppm tolerances";
    }
}
