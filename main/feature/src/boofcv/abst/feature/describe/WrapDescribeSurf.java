/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageSingleBand;


/**
 * @author Peter Abeles
 */
public class WrapDescribeSurf<T extends ImageSingleBand, II extends ImageSingleBand>
		implements DescribeRegionPoint<T,SurfFeature> {

	// computes SURF feature descriptor
	DescribePointSurf<II> surf;
	// integral image
	II ii;


	public WrapDescribeSurf(DescribePointSurf<II> surf )
	{
		this.surf = surf;
	}

	@Override
	public SurfFeature createDescription() {
		return new SurfFeature(surf.getDescriptionLength());
	}

	@Override
	public void setImage(T image) {
		if( ii != null ) {
			ii.reshape(image.width,image.height);
		}

		// compute integral image
		ii = GIntegralImageOps.transform(image,ii);
		surf.setImage(ii);
	}

	@Override
	public int getDescriptionLength() {
		return surf.getDescriptionLength();
	}

	@Override
	public boolean isInBounds(double x, double y, double orientation, double scale) {
		return true;
	}

	@Override
	public SurfFeature process(double x, double y, double orientation , double scale, SurfFeature ret) {

		if( ret == null )
			ret = createDescription();

		surf.describe(x,y, orientation, scale, ret);

		return ret;
	}

	@Override
	public boolean requiresScale() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public Class<SurfFeature> getDescriptionType() {
		return SurfFeature.class;
	}
}
