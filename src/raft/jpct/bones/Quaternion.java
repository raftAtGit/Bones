/**
 * Some of the classes in this file are adapted or inspired from Ardor3D. 
 * Such classes are indicated in class javadocs. 
 * 
 * Modification and redistribution of them may be subject to Ardor3D's license: 
 * http://www.ardor3d.com/LICENSE
 */
package raft.jpct.bones;

import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

/**
 * <p>Represents a rotation. Quaternion is a 4 value math object used to describe rotations. It has the advantage of being able
 * to avoid lock by adding a 4th dimension to rotation.</p>
 * 
 * <p>Note: This class is a stripped and modified version of Ardor3D's Quaternion to match jPCT. 
 * It may have bugs because of conversion. But especially {@link #fromVectorToVector(SimpleVector, SimpleVector)},
 * and Matrix in/out methods work ok.</p>
 *
 * <p>This class will possibly disappear once jPCT has its own Quaternion.</p>
 * 
 * <p>This class is adapted from <a href="http://www.ardor3d.com">Ardor3D.</a></p>
 */
public class Quaternion implements java.io.Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    /** A "close to zero" double epsilon value for use */
    private static final float EPSILON = 2.220446049250313E-16f;
    
    private static final SimpleVector ZERO_VECTOR = new SimpleVector();
    
    /**
     * x=0, y=0, z=0, w=1
     */
    public final static Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    public float x = 0;
    public float y = 0;
    public float z = 0;
    public float w = 1;

    /**
     * Constructs a new quaternion set to (0, 0, 0, 1).
     */
    public Quaternion() {
        this(IDENTITY);
    }

    /**
     * Constructs a new quaternion set to the (x, y, z, w) values of the given source quaternion.
     * 
     * @param source
     */
    public Quaternion(final Quaternion source) {
        this(source.x, source.y, source.z, source.w);
    }

    /**
     * Constructs a new quaternion set to (x, y, z, w).
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Quaternion(final float x, final float y, final float z, final float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Constructs a new quaternion by using rotation information from given matrix.
     * The matrix should be a valid rotation matrix. Otherwise result will be incorrect.
     * 
     *  @see #fromMatrix(Matrix)
     */
    public Quaternion(Matrix matrix) {
        fromMatrix(matrix);
    }
    
    /**
     * Sets the value of this quaternion to (x, y, z, w)
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     * @return this quaternion for chaining
     */
    public Quaternion set(final float x, final float y, final float z, final float w) {
    	this.x = x;
    	this.y = y;
    	this.z = z;
    	this.w = w;
        return this;
    }

    /**
     * Sets the value of this quaternion to the (x, y, z, w) values of the provided source quaternion.
     * 
     * @param source
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Quaternion set(final Quaternion source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.w = source.w;
        return this;
    }

    /**
     * Sets the value of this quaternion to the rotation described by the given matrix.
     * The matrix should be a valid rotation matrix. Otherwise result will be incorrect.
     * 
     * @param matrix
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Quaternion fromMatrix(final Matrix matrix) {
    	return fromAxes(matrix.getXAxis(), matrix.getYAxis(), matrix.getZAxis());
    }

    /**
     * <p>Sets the value of this quaternion to the rotation described by the given matrix values.</p>
     * <b>Note: </b> this method assumes column major matrix
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     * @return this quaternion for chaining
     */
    private Quaternion fromRotationMatrix(final float m00, final float m01, final float m02, final float m10,
            final float m11, final float m12, final float m20, final float m21, final float m22) {
        // Uses the Graphics Gems code, from
        // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
        // *NOT* the "Matrix and Quaternions FAQ", which has errors!

        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final float t = m00 + m11 + m22;

        // we protect the division by s by ensuring that s>=1
        double x, y, z, w;
        if (t >= 0) { // |w| >= .5
            double s = Math.sqrt(t + 1); // |s|>=1 ...
            w = 0.5 * s;
            s = 0.5 / s; // so this division isn't bad
            x = (m21 - m12) * s;
            y = (m02 - m20) * s;
            z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            double s = Math.sqrt(1.0 + m00 - m11 - m22); // |s|>=1
            x = s * 0.5; // |x| >= .5
            s = 0.5 / s;
            y = (m10 + m01) * s;
            z = (m02 + m20) * s;
            w = (m21 - m12) * s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0 + m11 - m00 - m22); // |s|>=1
            y = s * 0.5; // |y| >= .5
            s = 0.5 / s;
            x = (m10 + m01) * s;
            z = (m21 + m12) * s;
            w = (m02 - m20) * s;
        } else {
            double s = Math.sqrt(1.0 + m22 - m00 - m11); // |s|>=1
            z = s * 0.5; // |z| >= .5
            s = 0.5 / s;
            x = (m02 + m20) * s;
            y = (m21 + m12) * s;
            w = (m10 - m01) * s;
        }

        return set((float)x, (float)y, (float)z, (float)w);
    }


    /**
     * Sets the values of this quaternion to the values represented by a given angle and axis of rotation. Note that
     * this method normalizes given axis vector, so use fromAngleNormalAxis if your axis is already normalized. 
     * If axis == 0,0,0 the quaternion is set to identity.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if axis is null
     */
    public Quaternion fromAngleAxis(final float angle, final SimpleVector axis) {
        return fromAngleNormalAxis(angle, axis.normalize());
    }

    /**
     * Sets the values of this quaternion to the values represented by a given angle and unit length axis of rotation.
     * If axis == 0,0,0 the quaternion is set to identity.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized - unit length).
     * @throws NullPointerException
     *             if axis is null
     */
    public Quaternion fromAngleNormalAxis(final float angle, final SimpleVector axis) {
        if (axis.equals(ZERO_VECTOR)) {
            return setIdentity();
        }

        final float halfAngle = 0.5f * angle;
        final float sin = (float) Math.sin(halfAngle);
        final float w = (float) Math.cos(halfAngle);
        final float x = sin * axis.x;
        final float y = sin * axis.y;
        final float z = sin * axis.z;
        return set(x, y, z, w);
    }

    /**
     * Sets this quaternion to that which will rotate vector "from" into vector "to". from and to do not have to be the
     * same length.
     * 
     * @param from
     *            the source vector to rotate
     * @param to
     *            the destination vector into which to rotate the source vector
     * @return this quaternion for chaining
     */
    public Quaternion fromVectorToVector(final SimpleVector from, final SimpleVector to) {
        final SimpleVector a = from;
        final SimpleVector b = to;
        final float factor = a.length() * b.length();
        if (Math.abs(factor) > EPSILON) {
            
            final float dot = a.calcDot(b) / factor;
            final float theta = (float) Math.acos(Math.max(-1.0, Math.min(dot, 1.0)));
            final SimpleVector pivotVector = a.calcCross(b);
            
            if (dot < 0.0 && pivotVector.length() < EPSILON) {
                // Vectors parallel and opposite direction, therefore a rotation of 180 degrees about any vector
                // perpendicular to this vector will rotate vector a onto vector b.
                //
                // The following guarantees the dot-product will be 0.0.
                int dominantIndex;
                if (Math.abs(a.x) > Math.abs(a.y)) {
                    if (Math.abs(a.x) > Math.abs(a.z)) {
                        dominantIndex = 0;
                    } else {
                        dominantIndex = 2;
                    }
                } else {
                    if (Math.abs(a.y) > Math.abs(a.z)) {
                        dominantIndex = 1;
                    } else {
                        dominantIndex = 2;
                    }
                }
                setVectorValue(pivotVector, dominantIndex, -getVectorValue(a, (dominantIndex + 1) % 3));
                setVectorValue(pivotVector, (dominantIndex + 1) % 3, getVectorValue(a, dominantIndex));
                setVectorValue(pivotVector, (dominantIndex + 2) % 3, 0f);
            }
            return fromAngleAxis(theta, pivotVector);
        } else {
            return setIdentity();
        }
    }

    /**
     * 
     * Normalizes this quaternion. 
     * @return a new quaternion that represents a unit length version of this Quaternion.
     */
    public Quaternion normalize() {
        Quaternion result = new Quaternion();

        final float n = 1.f / magnitude();
        final float x = this.x * n;
        final float y = this.y * n;
        final float z = this.z * n;
        final float w = this.w * n;
        return result.set(x, y, z, w);
    }

    /**
     * Adds another Quaternion to this Quaternion.
     * 
     * @param quat the Quaternion to add
     * @return this Quaternion for chaining
     */
    public Quaternion add(final Quaternion quat) {
        this.x += quat.x;
        this.y += quat.y;
        this.z += quat.z;
        this.w += quat.w;
        return this;
    }

    /**
     * Calculates the difference vector of two Quaternions.
     * 
     * @param quat the second Quaternion
     * @return the difference vector between "this" and "quat"
     */
    public Quaternion calcSub(final Quaternion quat) {
        return new Quaternion(this.x - quat.x, this.y - quat.y, this.z - quat.z, this.w - quat.w);
    }

    /**
     * Multiplies each value of this quaternion by the given scalar value. The result is stored in this quaternion.
     * 
     * @param scalar
     *            the quaternion to multiply this quaternion by.
     * @return this quaternion for chaining.
     */
    public Quaternion scalarMul(final float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }
    
    public Quaternion multiply(Quaternion other) {
    	return multiply(other.x, other.y, other.z, other.w);
    }
    
    /**
     * Multiplies this quaternion by the supplied quaternion values. The result is stored locally.
     * 
     * @param qx
     * @param qy
     * @param qz
     * @param qw
     * @return this quaternion for chaining
     */
    public Quaternion multiply(final float qx, final float qy, final float qz, final float qw) {
        final float x = this.x * qw + this.y * qz - this.z * qy + this.w * qx;
        final float y = -this.x * qz + this.y * qw + this.z * qx + this.w * qy;
        final float z = this.x * qy - this.y * qx + this.z * qw + this.w * qz;
        final float w = -this.x * qx - this.y * qy - this.z * qz + this.w * qw;
        return set(x, y, z, w);
    }


    /**
     * Updates this quaternion to represent a rotation formed by the given three axes. These axes are assumed to be
     * orthogonal and no error checking is applied. It is the user's job to insure that the three axes being provided
     * indeed represent a proper right handed coordinate system.
     * 
     * @param axes
     *            the array containing the three vectors representing the coordinate system.
     * @return this quaternion for chaining
     * @throws IllegalArgumentException
     *             if the given axes array is smaller than 3 elements.
     */
    public Quaternion fromAxes(final SimpleVector[] axes) {
        if (axes.length < 3) {
            throw new IllegalArgumentException("axes array must have at least three elements");
        }
        return fromAxes(axes[0], axes[1], axes[2]);
    }

    /**
     * Updates this quaternion to represent a rotation formed by the given three axes. These axes are assumed to be
     * orthogonal and no error checking is applied. It is the user's job to insure that the three axes being provided
     * indeed represent a proper right handed coordinate system.
     * 
     * @param xAxis
     *            vector representing the x-axis of the coordinate system.
     * @param yAxis
     *            vector representing the y-axis of the coordinate system.
     * @param zAxis
     *            vector representing the z-axis of the coordinate system.
     * @return this quaternion for chaining
     */
    public Quaternion fromAxes(final SimpleVector xAxis, final SimpleVector yAxis, final SimpleVector zAxis) {
        return fromRotationMatrix(xAxis.x, yAxis.x, zAxis.x, xAxis.y, yAxis.y, zAxis.y,
                xAxis.z, yAxis.z, zAxis.z);
    }

    /**
     * Does a spherical linear interpolation between the given start and end quaternions by the given change amount.
     * Stores the result locally.
     * 
     * @param source
     * @param dest
     * @param weight
     * @return this quaternion for chaining.
     * 
     * 
     * @throws NullPointerException
     *             if startQuat or endQuat are null.
     */
    public Quaternion slerp(final Quaternion source, final Quaternion dest, final float weight) {
    	
        // Check for equality and skip operation.
        if (source.equals(dest)) {
            this.set(source);
            return this;
        }

        float result = source.calcDot(dest);
        //final Quaternion end = new Quaternion(dest);

        float endX = dest.x;
        float endY = dest.y;
        float endZ = dest.z;
        float endW = dest.w;
        
        if (result < 0.0) {
            // Negate the second quaternion and the result of the dot product
            //end.scalarMul(-1);
        	
        	endX = -endX;
        	endY = -endY;
        	endZ = -endZ;
        	endW = -endW;
        	
            result = -result;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - weight;
        float scale1 = weight;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double theta = Math.acos(result);
            final double invSinTheta = 1f / Math.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = (float) (Math.sin((1 - weight) * theta) * invSinTheta);
            scale1 = (float) (Math.sin((weight * theta)) * invSinTheta);
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        final float x = (scale0 * source.x) + (scale1 * endX);
        final float y = (scale0 * source.y) + (scale1 * endY);
        final float z = (scale0 * source.z) + (scale1 * endZ);
        final float w = (scale0 * source.w) + (scale1 * endW);
        set(x, y, z, w);

        // Return the interpolated quaternion
        return this;
    }

    /**
     * @return the squared magnitude of this quaternion.
     */
    public float magnitudeSquared() {
        return w * w + x * x + y * y + z * z;
    }

    /**
     * @return the magnitude of this quaternion. basically sqrt({@link #magnitude()})
     */
    public float magnitude() {
        final double magnitudeSQ = magnitudeSquared();
        if (magnitudeSQ == 1.0) {
            return 1f;
        }

        return (float)Math.sqrt(magnitudeSQ);
    }

    /**
     * @param quat
     * @return the dot product of this quaternion with the given quaternion.
     */
    public float calcDot(final Quaternion quat) {
        return this.x * quat.x + this.y * quat.y + this.z * quat.z + this.w * quat.w;
    }

    /**
     * Sets the value of this quaternion to (0, 0, 0, 1). Equivalent to calling set(0, 0, 0, 1)
     * 
     * @return this quaternion for chaining
     */
    public Quaternion setIdentity() {
        return set(0, 0, 0, 1);
    }

    /**
     * @return true if this quaternion is (0, 0, 0, 1)
     */
    public boolean isIdentity() {
        if (equals(IDENTITY)) {
            return true;
        }

        return false;
    }

    /**
     * Check a quaternion... if values are NaN or infinite, return false. Else return true.
     * @return true or false as stated above.
     */
    public boolean isValid() {
        if (Float.isNaN(this.x) || Float.isInfinite(this.x)) {
            return false;
        }
        if (Float.isNaN(this.y) || Float.isInfinite(this.y)) {
            return false;
        }
        if (Float.isNaN(this.z) || Float.isInfinite(this.z)) {
            return false;
        }
        if (Float.isNaN(this.w) || Float.isInfinite(this.w)) {
            return false;
        }
        return true;
    }

    /**
     * @return the string representation of this quaternion.
     */
    @Override
    public String toString() {
        return "Quaternion [X=" + this.x + ", Y=" + this.y + ", Z=" + this.z + ", W=" + this.w + "]";
    }

    /**
     * @return returns a unique code for this quaternion object based on its values. If two quaternions are numerically
     *         equal, they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        final long x = Double.doubleToLongBits(this.x);
        result += 31 * result + (int) (x ^ (x >>> 32));

        final long y = Double.doubleToLongBits(this.y);
        result += 31 * result + (int) (y ^ (y >>> 32));

        final long z = Double.doubleToLongBits(this.z);
        result += 31 * result + (int) (z ^ (z >>> 32));

        final long w = Double.doubleToLongBits(this.w);
        result += 31 * result + (int) (w ^ (w >>> 32));

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this quaternion and the provided quaternion have the same x, y, z and w values.
     */
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quaternion)) {
            return false;
        }
        final Quaternion comp = (Quaternion) o;
        return this.x == comp.x && this.y == comp.y && this.z == comp.z && this.w == comp.w;

    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    public Quaternion clone() {
        try {
            return (Quaternion) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }
    
    /**
     * @param index
     * @return x value if index == 0, y value if index == 1 or z value if index == 2
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    private float getVectorValue(SimpleVector v, int index) {
        switch (index) {
            case 0:
                return v.x;
            case 1:
                return v.y;
            case 2:
                return v.z;
        }
        throw new AssertionError("index: " + index);
    }
    
    private void setVectorValue(SimpleVector v, int index, float value) {
        switch (index) {
            case 0:
                v.x = value;
                return;
            case 1:
                v.y = value;
                return;
            case 2:
                v.z = value;
                return;
        }
        throw new AssertionError("index: " + index);
    }

    /** 
     * Returns matrix representation of this quaternion.
    * @return the rotation matrix representation of this quaternion (normalized)
    */
	public Matrix getRotationMatrix() {
		return setRotation(new Matrix());
	}
	
    /**
     * Sets rotation part of given matrix to rotation represented by this quaternion.
     * @param matrix 
     *            the matrix to store the result
     * @return the rotation matrix representation of this quaternion (normalized)
     */
	public Matrix setRotation(Matrix matrix) {
        final float norm = magnitudeSquared();
        final float s = (norm > 0.0 ? 2.0f / norm : 0.0f);

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final float xs = this.x * s;
        final float ys = this.y * s;
        final float zs = this.z * s;
        final float xx = this.x * xs;
        final float xy = this.x * ys;
        final float xz = this.x * zs;
        final float xw = this.w * xs;
        final float yy = this.y * ys;
        final float yz = this.y * zs;
        final float yw = this.w * ys;
        final float zz = this.z * zs;
        final float zw = this.w * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        matrix.set(0, 0, 1f - (yy + zz));
        matrix.set(1, 0, xy - zw);
        matrix.set(2, 0, xz + yw);
        matrix.set(0, 1, xy + zw);
        matrix.set(1, 1, 1f - (xx + zz));
        matrix.set(2, 1, yz - xw);
        matrix.set(0, 2, xz - yw);
        matrix.set(1, 2, yz + xw);
        matrix.set(2, 2, 1f - (xx + yy));

        return matrix;
    
	}
	
	/** 
	 * Rotates the quaternion around X axis. 
	 * uses {@link Matrix#rotateX(float)} behind the scenes. 
	 * */
	public Quaternion rotateX(float angle) {
		Matrix m = getRotationMatrix();
		m.rotateX(angle);
		fromMatrix(m);
		return this;
	}

	/** 
	 * Rotates the quaternion around Y axis. 
	 * uses {@link Matrix#rotateY(float)} behind the scenes. 
	 * */
	public Quaternion rotateY(float angle) {
		Matrix m = getRotationMatrix();
		m.rotateY(angle);
		fromMatrix(m);
		return this;
	}
	
	/** 
	 * Rotates the quaternion around Z axis. 
	 * uses {@link Matrix#rotateZ(float)} behind the scenes. 
	 * */
	public Quaternion rotateZ(float angle) {
		Matrix m = getRotationMatrix();
		m.rotateZ(angle);
		fromMatrix(m);
		return this;
	}

	/** 
	 * Applies rotation of given matrix to this quaternion. 
	 * uses {@link Matrix#matMul(Matrix)} behind the scenes. 
	 * */
	public Quaternion rotate(Matrix matrix) {
		Matrix m = getRotationMatrix();
		m.matMul(matrix);
		fromMatrix(m);
		return this;
	}
	
    /**
     * <code>inverse</code> returns the inverse of this quaternion as a new
     * quaternion. If this quaternion does not have an inverse (if its normal is
     * 0 or less), then an IllegalStateException is thrown.
     *
     * @return the inverse of this quaternion or null if the inverse does not
     *         exist.
     * @throws IllegalStateException if can not be inverted
     */
    public Quaternion invert() {
        float norm = norm();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            return new Quaternion(-x * invNorm, -y * invNorm, -z * invNorm, w * invNorm);
        } 
        throw new IllegalStateException("inverse does not exist: norm=" + norm);       
    }
	
    /**
     * <code>norm</code> returns the norm of this quaternion. This is the dot
     * product of this quaternion with itself.
     *
     * @return the norm of the quaternion.
     */
    public float norm() {
        return w * w + x * x + y * y + z * z;
    }

}
