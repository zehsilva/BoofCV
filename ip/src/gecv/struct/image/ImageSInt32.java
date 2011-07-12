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

package gecv.struct.image;

/**
 * <p>
 * An image where the primitive type is a signed 32-bit int.  By default all operations treat elements
 * in this image as an unsigned bytes.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageSInt32 extends ImageInteger<ImageSInt32> {

	public int data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageSInt32(int width, int height) {
		super(width, height);
	}

	public ImageSInt32() {
	}

	@Override
	protected int unsafe_get(int x, int y) {
		return data[getIndex(x, y)];
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	@Override
	public void set(int x, int y, int value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		data[getIndex(x, y)] = (short) value;
	}

	@Override
	public ImageTypeInfo<ImageSInt32> getTypeInfo() {
		return ImageTypeInfo.S32;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (int[]) data;
	}

	@Override
	public ImageSInt32 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new ImageSInt32();
		return new ImageSInt32(imgWidth, imgHeight);
	}
}