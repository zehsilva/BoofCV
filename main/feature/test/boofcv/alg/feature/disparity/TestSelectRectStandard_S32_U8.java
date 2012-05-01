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

package boofcv.alg.feature.disparity;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSelectRectStandard_S32_U8 {

	int w=20;
	int h=25;
	int maxDisparity=10;

	ImageUInt8 disparity = new ImageUInt8(w,h);

	@Test
	public void basic() {
		BasicDisparitySelectRectTests<ImageUInt8> test =
				new BasicDisparitySelectRectTests<ImageUInt8>(ImageUInt8.class) {
					@Override
					public DisparitySelect_S32<ImageUInt8> createAlg() {
						return new SelectRectStandard_S32_U8(-1,-1,-1);
					}
				};

		test.simpleTest();
	}

	@Test
	public void maxError() {
		int y = 3;

		SelectRectStandard_S32_U8 alg = new SelectRectStandard_S32_U8(2,-1,-1);
		alg.configure(disparity,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = d==0 ? 5 : x;
			}
		}

		alg.process(y, scores);

		// Below error threshold and disparity of 1 should be optimal
		assertEquals(1, GeneralizedImageOps.get(disparity, 1+2, y), 1e-8);
		assertEquals(1, GeneralizedImageOps.get(disparity, 2+2, y), 1e-8);
		// At this point the error should become too high
		assertEquals(0, GeneralizedImageOps.get(disparity, 3+2, y), 1e-8);
		assertEquals(0, GeneralizedImageOps.get(disparity, 4+2, y), 1e-8);

		// Sanity check, much higher error threshold
		alg = new SelectRectStandard_S32_U8(20,-1,-1);
		alg.configure(disparity,maxDisparity,2);
		alg.process(y,scores);
		assertEquals(1, GeneralizedImageOps.get(disparity, 3+2, y), 1e-8);
		assertEquals(1, GeneralizedImageOps.get(disparity, 4+2, y), 1e-8);
	}

	/**
	 * Similar to simpleTest but takes in account the effects of right to left validation
	 */
	@Test
	public void testRightToLeftValidation() {

		int y = 3;

		SelectRectStandard_S32_U8 alg = new SelectRectStandard_S32_U8(-1,1,-1);
		alg.configure(disparity,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = Math.abs(d-5);
			}
		}

		alg.process(y,scores);

		// These should all be zero since other pixels will have lower scores
		for( int i = 0; i < 4; i++ )
			assertEquals(0, GeneralizedImageOps.get(disparity, i+2, y), 1e-8);
		// the tolerance is one, so this should be 4
		assertEquals(4, GeneralizedImageOps.get(disparity, 4+2, y), 1e-8);
		// should be at 5 for the remainder
		for( int i = 5; i < w-4; i++ )
			assertEquals(5, GeneralizedImageOps.get(disparity, i+2, y), 1e-8);

		// sanity check, I now set the tolerance to zero
		alg = new SelectRectStandard_S32_U8(-1,0,-1);
		alg.configure(disparity,maxDisparity,2);
		alg.process(y,scores);
		assertEquals(0, GeneralizedImageOps.get(disparity, 4+2, y), 1e-8);
	}

	/**
	 * Test the confidence in a region with very similar cost score (little texture)
	 */
	@Test
	public void confidenceFlatRegion() {
		int minValue = 3;
		int y = 3;

		SelectRectStandard_S32_U8 alg = new SelectRectStandard_S32_U8(-1,-1,3);
		alg.configure(disparity,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = minValue + Math.abs(2-d);
			}
		}

		alg.process(y,scores);

		// it should reject the solution
		assertEquals(0, GeneralizedImageOps.get(disparity, 4+2, y), 1e-8);
	}

	/**
	 * There are two similar peaks.  Repeated pattern
	 */
	@Test
	public void confidenceMultiplePeak() {
		confidenceMultiplePeak(3);
		confidenceMultiplePeak(0);
	}

	private void confidenceMultiplePeak(int minValue) {
		int y = 3;

		SelectRectStandard_S32_U8 alg = new SelectRectStandard_S32_U8(-1,-1,3);
		alg.configure(disparity,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = minValue + (d % 3);
			}
		}

		alg.process(y,scores);

		// it should reject the solution
		assertEquals(0, GeneralizedImageOps.get(disparity, 4 + 2, y), 1e-8);
	}

}
