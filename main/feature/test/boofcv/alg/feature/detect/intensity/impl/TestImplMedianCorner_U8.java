/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.GenericCornerIntensityTests;
import boofcv.alg.feature.detect.intensity.MedianCornerIntensity;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestImplMedianCorner_U8 extends GenericCornerIntensityTests {

	ImageUInt8 median = new ImageUInt8(width,height);

	@Test
	public void genericTests() {
		performAllTests();
	}

	@Override
	public void computeIntensity( ImageFloat32 intensity ) {
		MedianCornerIntensity.process(intensity,imageI,median);
	}

	@Override
	protected void computeDerivatives() {
		FactoryBlurFilter.median(ImageUInt8.class,2).process(imageI,median);
	}
}
