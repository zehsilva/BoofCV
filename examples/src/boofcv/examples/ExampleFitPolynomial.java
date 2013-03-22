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

package boofcv.examples;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.feature.shapes.ShapeFittingOps;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * Demonstration of how to convert a point sequence describing an objects outline/contour into a sequence of line
 * segments.  Useful when analysing shapes such as squares and triangles or when trying to simply the low level
 * pixel output.
 *
 * @author Peter Abeles
 */
public class ExampleFitPolynomial {

	/**
	 * Fits a polygons the found contours around binary blobs.  This demonstrates how it can be used to handle
	 * connected loops of points
	 */
	public static void fitContours( ImageFloat32 input ) {

		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		BufferedImage polygon = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		double mean = ImageStatistics.mean(input);

		// create a binary image by thresholding
		ThresholdImageOps.threshold(input, binary, (float) mean, true);

		// Find the contour around the shapes
		List<Contour> contours = BinaryImageOps.contour(binary,8,null);

		// Fit a polygon to each shape and draw the results
		Graphics2D g2 = polygon.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for( Contour c : contours ) {
			List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,true,1,Math.PI/10,100);

			g2.setColor(Color.RED);
			drawPolygon(vertexes,true,g2);

			g2.setColor(Color.BLUE);
			for( List<Point2D_I32> internal : c.internal ) {
				vertexes = ShapeFittingOps.fitPolygon(internal,true,1,Math.PI/10,100);
				drawPolygon(vertexes,true,g2);
			}
		}

		ShowImages.showWindow(polygon,"Contours");
	}

	/**
	 * Fits a sequence of line-segments into a sequence of points found using the Canny edge detector.  In this case
	 * the points are not connected.
	 */
	public static void fitEdges( ImageFloat32 input ) {

		BufferedImage displayImage = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);

		// Finds edges inside the image
		CannyEdge<ImageFloat32,ImageFloat32> canny =
				FactoryEdgeDetectors.canny(2, true, true, ImageFloat32.class, ImageFloat32.class);

		canny.process(input,0.1f,0.3f,null);
		List<EdgeContour> contours = canny.getContours();

		Graphics2D g2 = displayImage.createGraphics();
		g2.setStroke(new BasicStroke(2));

		// used to select colors for each line
		Random rand = new Random(234);

		for( EdgeContour e : contours ) {
			g2.setColor(new Color(rand.nextInt()));

			for(EdgeSegment s : e.segments ) {
				// fit line segments to the point sequence.  Note that loop is false
				List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points,false,1,Math.PI/10,100);

				drawPolygon(vertexes,false,g2);
			}
		}

		ShowImages.showWindow(displayImage,"Edges");
	}


	public static void main( String args[] ) {
		// load and convert the image into a usable format
		BufferedImage image = UtilImageIO.loadImage("../data/evaluation/amoeba_shapes.jpg");
		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);

		ShowImages.showWindow(image,"Original");

		fitContours(input);
		fitEdges(input);
	}

	/**
	 * Draws a polygon
	 */
	private static void drawPolygon( List<PointIndex_I32> vertexes , boolean loop, Graphics2D g2 ) {
		for( int i = 0; i < vertexes.size()-1; i++ ) {
			Point2D_I32 p0 = vertexes.get(i);
			Point2D_I32 p1 = vertexes.get(i+1);
			g2.drawLine(p0.x,p0.y,p1.x,p1.y);
		}
		if( loop ) {
			Point2D_I32 p0 = vertexes.get(0);
			Point2D_I32 p1 = vertexes.get(vertexes.size()-1);
			g2.drawLine(p0.x,p0.y,p1.x,p1.y);
		}
	}
}
