/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.alg.transform.pyramid.GradientPyramid;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ImagePyramidPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidI;
import gecv.struct.pyramid.PyramidUpdater;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */

public abstract class TrackVideoPyramidKLT<I extends ImageBase, D extends ImageBase>
		extends ProcessImageSequence<I> {

	private PkltManager<I, D> tracker;

	ImagePanel panel;
	ImagePyramidPanel pyramidPanel;
	int totalRespawns;

	GradientPyramid<I,D> updateGradient;

	ImagePyramid<I> basePyramid;
	ImagePyramid<D> derivX;
	ImagePyramid<D> derivY;


	@SuppressWarnings({"unchecked"})
	public TrackVideoPyramidKLT(SimpleImageSequence<I> sequence,
								PkltManager<I, D> tracker ,
								PyramidUpdater<I> pyramidUpdater ,
								GradientPyramid<I,D> updateGradient) {
		super(sequence);
		this.tracker = tracker;
		this.updateGradient = updateGradient;
		PkltManagerConfig<I, D> config = tracker.getConfig();

		// declare the image pyramid
		basePyramid = new ImagePyramidI<I>(true,pyramidUpdater,config.pyramidScaling);
		derivX = new ImagePyramidI<D>(false,null,config.pyramidScaling);
		derivY = new ImagePyramidI<D>(false,null,config.pyramidScaling);
	}


	@Override
	public void processFrame(I image) {

		basePyramid.update(image);
		updateGradient.update(basePyramid,derivX,derivY);

		tracker.processFrame(basePyramid,derivX,derivY);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, I origImage) {
		Graphics2D g2 = guiImage.createGraphics();
		
		drawFeatures(g2, tracker.getTracks(), Color.RED);
		drawFeatures(g2, tracker.getSpawned(), Color.BLUE);

		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "KLT Pyramidal Tracker");
			addComponent(panel);
		} else {
			panel.setBufferedImage(guiImage);
			panel.repaint();
		}

//		if( pyramidPanel == null ) {
//			pyramidPanel = new ImagePyramidPanel(tracker.getPyramid());
//			ShowImages.showWindow(pyramidPanel,"Pyramid");
//			addComponent(pyramidPanel);
//		} else {
//			pyramidPanel.render();
//			pyramidPanel.repaint();
//		}

		if( tracker.getSpawned().size() != 0 )
			totalRespawns++;
		System.out.println(" total features: "+tracker.getTracks().size()+" totalRespawns "+totalRespawns);
	}

	private void drawFeatures(Graphics2D g2,
							  java.util.List<PyramidKltFeature> list,
							  Color color ) {
		int r = 3;
		int w = r*2+1;
		int ro = r+2;
		int wo = ro*2+1;

		for (int i = 0; i < list.size(); i++) {
			PyramidKltFeature pt = list.get(i);

			int x = (int)pt.x;
			int y = (int)pt.y;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - ro, y - ro, wo, wo);
			g2.setColor(color);
			g2.fillOval(x - r, y - r, w, w);
		}
	}
}
