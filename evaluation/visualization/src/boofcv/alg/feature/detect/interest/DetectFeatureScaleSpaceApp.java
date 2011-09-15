/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.interest.InterestPointScaleSpace;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.feature.ScaleSpacePointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a window showing selected features across scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeatureScaleSpaceApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmPanel
{

//	static String fileName = "data/outdoors01.jpg";
	static String fileName = "data/sunflowers.png";
//	static String fileName = "data/particles01.jpg";
//	static String fileName = "data/scale/beach02.jpg";
//	static String fileName = "data/indoors01.jpg";
//	static String fileName = "data/shapes01.png";
//	static String fileName = "data/stitch/cave_01.jpg";

	static int NUM_FEATURES = 400;
	int r = 2;
	GaussianScaleSpace<T,D> ss;
	Class<T> imageType;
	ScaleSpacePointPanel panel;
	boolean hasImage = false;

	public DetectFeatureScaleSpaceApp( Class<T> imageType , Class<D> derivType ) {
		this.imageType = imageType;

		addAlgorithm("Hessian",FactoryInterestPointAlgs.hessianScaleSpace(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm("Hessian Laplace",FactoryInterestPointAlgs.hessianLaplace(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm("Harris",FactoryInterestPointAlgs.harrisScaleSpace(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm("Harris Laplace",FactoryInterestPointAlgs.harrisLaplace(r,1,NUM_FEATURES,imageType,derivType));

		ss = FactoryGaussianScaleSpace.nocache(imageType);
		ss.setScales(1,1.5,2,4,8,12,24);

		panel = new ScaleSpacePointPanel(ss,r);

		add(panel, BorderLayout.CENTER);
	}

	public synchronized void process( BufferedImage input ) {
		T workImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		ss.setImage(workImage);
		panel.setBackground(input);
		hasImage = true;
		refreshAlgorithm();
	}

	@Override
	public synchronized void setActiveAlgorithm(String name, Object cookie) {
		if( !hasImage )
			return;

		final InterestPointScaleSpace<T,D> det = (InterestPointScaleSpace<T,D>)cookie;
		det.detect(ss);
		panel.setPoints(det.getInterestPoints());
		panel.repaint();
		panel.requestFocusInWindow();
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);
		DetectFeatureScaleSpaceApp app = new DetectFeatureScaleSpaceApp(ImageFloat32.class,ImageFloat32.class);
		app.process(input);
		ShowImages.showWindow(app,"Feature Scale Space");

		System.out.println("Done");
	}
}