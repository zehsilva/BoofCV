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

package boofcv.factory.feature.tracker;

import boofcv.abst.feature.associate.*;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.describe.WrapDescribeBrief;
import boofcv.abst.feature.describe.WrapDescribePixelRegionNCC;
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.ConfigAverageIntegral;
import boofcv.abst.feature.orientation.ConfigSlidingIntegral;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.feature.tracker.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.interest.EasyGeneralFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.*;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;

import java.util.Random;


/**
 * Factory for creating trackers which implement {@link boofcv.abst.feature.tracker.PointTracker}.  These trackers
 * are intended for use in SFM applications.  Some features which individual trackers can provide are lost when
 * using the high level interface {@link PointTracker}.  To create low level tracking algorithms see
 * {@link FactoryTrackerAlg}
 *
 * @see FactoryTrackerAlg
 *
 * @author Peter Abeles
 */
public class FactoryPointTracker {

	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @see boofcv.alg.tracker.klt.PyramidKltTracker
	 *
	 * @param scaling       Scales in the image pyramid. Recommend [1,2,4] or [2,4]
	 * @param configExtract Configuration for extracting features
	 * @param featureRadius Size of the tracked feature.  Try 3 or 5
	 * @param imageType     Input image type.
	 * @param derivType     Image derivative  type.
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PointTracker<I> klt(int scaling[], ConfigGeneralDetector configExtract, int featureRadius,
							 Class<I> imageType, Class<D> derivType) {
		PkltConfig<I, D> config =
				PkltConfig.createDefault(imageType, derivType);
		config.pyramidScaling = scaling;
		config.templateRadius = featureRadius;

		return klt(config, configExtract);
	}

	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @see boofcv.alg.tracker.klt.PyramidKltTracker
	 *
	 * @param config Config for the tracker. Try PkltConfig.createDefault().
	 * @param configExtract Configuration for extracting features
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PointTracker<I> klt(PkltConfig<I, D> config, ConfigGeneralDetector configExtract) {

		GeneralFeatureDetector<I, D> detector = createShiTomasi(configExtract, config.typeDeriv);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(config.typeInput);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(config.typeDeriv);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(config.typeInput, config.typeDeriv);

		PyramidDiscrete<I> pyramid = FactoryPyramid.discreteGaussian(config.pyramidScaling,-1,2,true,config.typeInput);

		return new PointTrackerKltPyramid<I, D>(config.config,config.templateRadius,pyramid,detector,
				gradient,interpInput,interpDeriv,config.typeDeriv);
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features and describes them with SURF using the faster variant
	 * of SURF.
	 *
	 * @see DescribePointSurf
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param configDetector Configuration for SURF detector
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for orientation
	 * @param imageType      Type of image the input is.
	 * @return SURF based tracker.
	 */
	// TODO remove maxTracks?  Use number of detected instead
	public static <I extends ImageSingleBand>
	PointTracker<I> dda_FH_SURF_Fast(
										  ConfigFastHessian configDetector ,
										  ConfigSurfDescribe.Speed configDescribe ,
										  ConfigAverageIntegral configOrientation ,
										  Class<I> imageType)
	{
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 5, true));

		AssociateDescription2D<SurfFeature> generalAssoc =
				new AssociateDescTo2D<SurfFeature>(new WrapAssociateSurfBasic(assoc));

		DetectDescribePoint<I,SurfFeature> fused =
				FactoryDetectDescribe.surfFast(configDetector, configDescribe, configOrientation, imageType);

		DdaManagerDetectDescribePoint<I,SurfFeature> manager = new DdaManagerDetectDescribePoint<I,SurfFeature>(fused);

		return new DetectDescribeAssociate<I,SurfFeature>(manager, generalAssoc,false);
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features and describes them with SURF using the faster variant
	 * of SURF.
	 *
	 * @see DescribePointSurf
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param configDetector Configuration for SURF detector
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for orientation
	 * @param imageType      Type of image the input is.
	 * @return SURF based tracker.
	 */
	// TODO remove maxTracks?  Use number of detected instead
	public static <I extends ImageSingleBand>
	PointTracker<I> dda_FH_SURF_Stable(
											ConfigFastHessian configDetector ,
											ConfigSurfDescribe.Stablility configDescribe ,
											ConfigSlidingIntegral configOrientation ,
											Class<I> imageType)
	{
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 5, true));

		AssociateDescription2D<SurfFeature> generalAssoc =
				new AssociateDescTo2D<SurfFeature>(new WrapAssociateSurfBasic(assoc));

		DetectDescribePoint<I,SurfFeature> fused =
				FactoryDetectDescribe.surfStable(configDetector,configDescribe,configOrientation,imageType);

		DdaManagerDetectDescribePoint<I,SurfFeature> manager = new DdaManagerDetectDescribePoint<I,SurfFeature>(fused);

		return new DetectDescribeAssociate<I,SurfFeature>(manager, generalAssoc,false);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with BRIEF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointBrief
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param maxAssociationError Maximum allowed association error.  Try 200.
	 * @param configExtract Configuration for extracting features
	 * @param imageType           Type of image being processed.
	 * @param derivType Type of image used to store the image derivative. null == use default
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PointTracker<I> dda_ST_BRIEF(int maxAssociationError,
									  ConfigGeneralDetector configExtract,
									  Class<I> imageType, Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DescribePointBrief<I> brief = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		GeneralFeatureDetector<I, D> detectPoint = createShiTomasi(configExtract, derivType);
		EasyGeneralFeatureDetector<I,D> easy = new EasyGeneralFeatureDetector<I, D>(detectPoint,imageType,derivType);

		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		AssociateDescription2D<TupleDesc_B> association =
				new AssociateDescTo2D<TupleDesc_B>(FactoryAssociation.greedy(score, maxAssociationError, true));

		DdaManagerGeneralPoint<I,D,TupleDesc_B> manager =
				new DdaManagerGeneralPoint<I,D,TupleDesc_B>(easy,new WrapDescribeBrief<I>(brief),1.0);

		return new DetectDescribeAssociate<I,TupleDesc_B>(manager, association,false);
	}

	/**
	 * Creates a tracker which detects FAST corner features and describes them with BRIEF.
	 *
	 * @see boofcv.alg.feature.detect.intensity.FastCornerIntensity
	 * @see DescribePointBrief
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param configFast Configuration for FAST detector
	 * @param configExtract Configuration for extracting features
	 * @param maxAssociationError Maximum allowed association error.  Try 200.
	 * @param imageType           Type of image being processed.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PointTracker<I> dda_FAST_BRIEF(ConfigFast configFast,
								   ConfigGeneralDetector configExtract,
								   int maxAssociationError,
								   Class<I> imageType )
	{
		DescribePointBrief<I> brief = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		GeneralFeatureDetector<I,D> corner = FactoryDetectPoint.createFast(configFast, configExtract, imageType);
		EasyGeneralFeatureDetector<I,D> easy = new EasyGeneralFeatureDetector<I, D>(corner,imageType,null);

		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		AssociateDescription2D<TupleDesc_B> association =
				new AssociateDescTo2D<TupleDesc_B>(
						FactoryAssociation.greedy(score, maxAssociationError, true));

		DdaManagerGeneralPoint<I,D,TupleDesc_B> manager =
				new DdaManagerGeneralPoint<I,D,TupleDesc_B>(easy,new WrapDescribeBrief<I>(brief),1.0);

		return new DetectDescribeAssociate<I,TupleDesc_B>(manager, association,false);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features and describes them with NCC.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointPixelRegionNCC
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param configExtract Configuration for extracting features
	 * @param describeRadius Radius of the region being described.  Try 2.
	 * @param imageType      Type of image being processed.
	 * @param derivType      Type of image used to store the image derivative. null == use default     */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PointTracker<I> dda_ST_NCC(ConfigGeneralDetector configExtract, int describeRadius,
									Class<I> imageType, Class<D> derivType) {

		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		int w = 2*describeRadius+1;

		DescribePointPixelRegionNCC<I> alg = FactoryDescribePointAlgs.pixelRegionNCC(w, w, imageType);

		GeneralFeatureDetector<I, D> corner = createShiTomasi(configExtract, derivType);
		EasyGeneralFeatureDetector<I,D> easy = new EasyGeneralFeatureDetector<I, D>(corner,imageType,derivType);

		ScoreAssociateNccFeature score = new ScoreAssociateNccFeature();

		AssociateDescription2D<NccFeature> association =
				new AssociateDescTo2D<NccFeature>(
						FactoryAssociation.greedy(score, Double.MAX_VALUE, true));

		DdaManagerGeneralPoint<I,D,NccFeature> manager =
				new DdaManagerGeneralPoint<I,D,NccFeature>(easy,new WrapDescribePixelRegionNCC<I>(alg),1.0);

		return new DetectDescribeAssociate<I,NccFeature>(manager, association,false);
	}

	/**
	 * Creates a tracker which uses the detect, describe, associate architecture.
	 *
	 * @param detector Interest point detector.
	 * @param orientation Optional orientation estimation algorithm. Can be null.
	 * @param describe Region description.
	 * @param associate Description association.
	 * @param updateDescription After a track has been associated should the description be changed?  Try false.
	 * @param <I> Type of input image.
	 * @param <Desc> Type of region description
	 * @return tracker
	 */
	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	DetectDescribeAssociate<I,Desc> dda(InterestPointDetector<I> detector,
										OrientationImage<I> orientation ,
										DescribeRegionPoint<I, Desc> describe,
										AssociateDescription2D<Desc> associate ,
										boolean updateDescription ) {

		DetectDescribeFusion<I,Desc> fused =
				new DetectDescribeFusion<I,Desc>(detector,orientation,describe);

		DdaManagerDetectDescribePoint<I,Desc> manager =
				new DdaManagerDetectDescribePoint<I,Desc>(fused);

		DetectDescribeAssociate<I,Desc> dat =
				new DetectDescribeAssociate<I,Desc>(manager, associate,updateDescription);

		return dat;
	}

	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	DetectDescribeAssociate<I,Desc> dda( DetectDescribePoint<I, Desc> detDesc,
										AssociateDescription2D<Desc> associate ,
										boolean updateDescription ) {

		DdaManagerDetectDescribePoint<I,Desc> manager =
				new DdaManagerDetectDescribePoint<I,Desc>(detDesc);

		DetectDescribeAssociate<I,Desc> dat =
				new DetectDescribeAssociate<I,Desc>(manager, associate,updateDescription);

		return dat;
	}

	/**
	 * Creates a tracker which detects Fast-Hessian features, describes them with SURF, nominally tracks them using KLT.
	 *
	 * @see DescribePointSurf
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param trackRadius Size of feature being tracked by KLT
	 * @param pyramidScalingKlt Image pyramid used for KLT
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param configDetector Configuration for SURF detector
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for region orientation
	 * @param imageType      Type of image the input is.
	 * @param <I>            Input image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand>
	PointTracker<I> combined_FH_SURF_KLT(int trackRadius,
													  int[] pyramidScalingKlt ,
													  int reactivateThreshold ,
													  ConfigFastHessian configDetector ,
													  ConfigSurfDescribe.Stablility configDescribe ,
													  ConfigSlidingIntegral configOrientation ,
													  Class<I> imageType) {

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.defaultScore(TupleDesc_F64.class);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, true));

		AssociateDescription<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		DetectDescribePoint<I,SurfFeature> fused =
				FactoryDetectDescribe.surfStable(configDetector, configDescribe, configOrientation, imageType);

		return combined(fused,generalAssoc,trackRadius,pyramidScalingKlt,reactivateThreshold,
				imageType);
	}

	/**
	 * Creates a tracker which detects Shi-Tomasi corner features, describes them with SURF, and
	 * nominally tracks them using KLT.
	 *
	 * @see boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity
	 * @see DescribePointSurf
	 * @see boofcv.abst.feature.tracker.DdaManagerDetectDescribePoint
	 *
	 * @param configExtract Configuration for extracting features
	 * @param trackRadius Size of feature being tracked by KLT
	 * @param pyramidScalingKlt Image pyramid used for KLT
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param configDescribe Configuration for SURF descriptor
	 * @param configOrientation Configuration for region orientation.  If null then orientation isn't estimated
	 * @param imageType      Type of image the input is.
	 * @param derivType      Image derivative type.
	 * @param <I>            Input image type.
	 * @return SURF based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PointTracker<I> combined_ST_SURF_KLT(ConfigGeneralDetector configExtract,
													  int trackRadius,
													  int[] pyramidScalingKlt ,
													  int reactivateThreshold ,
													  ConfigSurfDescribe.Stablility configDescribe,
													  ConfigSlidingIntegral configOrientation ,
													  Class<I> imageType,
													  Class<D> derivType ) {

		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		GeneralFeatureDetector<I, D> corner = createShiTomasi(configExtract, derivType);
		InterestPointDetector<I> detector = FactoryInterestPoint.wrapPoint(corner, 1, imageType, derivType);

		DescribeRegionPoint<I,SurfFeature> regionDesc
				= FactoryDescribeRegionPoint.surfStable(configDescribe, imageType);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
		AssociateSurfBasic assoc = new AssociateSurfBasic(FactoryAssociation.greedy(score, 100000, true));

		AssociateDescription<SurfFeature> generalAssoc = new WrapAssociateSurfBasic(assoc);

		OrientationImage<I> orientation = null;

		if( configOrientation != null ) {
			Class integralType = GIntegralImageOps.getIntegralType(imageType);
			OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(configOrientation, integralType);
			orientation = FactoryOrientation.convertImage(orientationII,imageType);
		}

		return combined(detector,orientation,regionDesc,generalAssoc,trackRadius,pyramidScalingKlt,reactivateThreshold,
				imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector.
	 * @param orientation Optional feature orientation.  Can be null.
	 * @param describe Feature description
	 * @param associate Association algorithm.
	 * @param featureRadiusKlt KLT feature radius
	 * @param pyramidScalingKlt KLT pyramid configuration
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param imageType Input image type.
	 * @param <I> Input image type.
	 * @param <Desc> Feature description type.
	 * @return Feature tracker
	 */
	public static <I extends ImageSingleBand, Desc extends TupleDesc>
	PointTracker<I> combined( InterestPointDetector<I> detector,
							  OrientationImage<I> orientation ,
							  DescribeRegionPoint<I, Desc> describe,
							  AssociateDescription<Desc> associate ,
							  int featureRadiusKlt,
							  int[] pyramidScalingKlt ,
							  int reactivateThreshold,
							  Class<I> imageType )
	{
		DetectDescribeFusion<I,Desc> fused = new DetectDescribeFusion<I,Desc>(detector,orientation,describe);

		return combined(fused,associate,featureRadiusKlt,pyramidScalingKlt,reactivateThreshold,imageType);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector and describer.
	 * @param associate Association algorithm.
	 * @param featureRadiusKlt KLT feature radius
	 * @param pyramidScalingKlt KLT pyramid configuration
	 * @param reactivateThreshold Tracks are reactivated after this many have been dropped.  Try 10% of maxMatches
	 * @param imageType Input image type.
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @param <Desc> Feature description type.
	 * @return Feature tracker
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand, Desc extends TupleDesc>
	PointTracker<I> combined( DetectDescribePoint<I,Desc> detector ,
								   AssociateDescription<Desc> associate ,
								   int featureRadiusKlt,
								   int[] pyramidScalingKlt ,
								   int reactivateThreshold,
								   Class<I> imageType )
	{
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		CombinedTrackerScalePoint<I, D,Desc> tracker =
				FactoryTrackerAlg.combined(detector,associate,featureRadiusKlt,pyramidScalingKlt,
						imageType,derivType);

		return new PointTrackerCombined<I,D,Desc>(tracker,reactivateThreshold,imageType,derivType);
	}


	public static <I extends ImageSingleBand, D extends ImageSingleBand, Desc extends TupleDesc>
	PointTracker<I> dda(GeneralFeatureDetector<I, D> detector,
						DescribeRegionPoint<I, Desc> describe,
						AssociateDescription2D<Desc> associate,
						double scale,
						Class<I> imageType) {

		EasyGeneralFeatureDetector<I,D> easy = new EasyGeneralFeatureDetector<I, D>(detector,imageType,null);

		DdaManagerGeneralPoint<I,D,Desc> manager =
				new DdaManagerGeneralPoint<I,D,Desc>(easy,describe,scale);

		return new DetectDescribeAssociate<I,Desc>(manager,associate,false);
	}

	/**
	 * Creates a Shi-Tomasi corner detector specifically designed for SFM.  Smaller feature radius work better.
	 * Variable detectRadius to control the number of features.  When larger features are used weighting should
	 * be set to true, but because this is so small, it is set to false
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureDetector<I, D> createShiTomasi(ConfigGeneralDetector config ,
												 Class<D> derivType)
	{
		GradientCornerIntensity<D> cornerIntensity = FactoryIntensityPointAlg.shiTomasi(1, false, derivType);

		return FactoryDetectPoint.createGeneral(cornerIntensity, config );
	}
}