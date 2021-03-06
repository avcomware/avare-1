/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.instruments;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/***
 * Implementation of a Vertical Approach Slope Indicator
 */
public class VASI {
	Paint mVASIPaint;	// Our very own paint object
	int   mBarCount;	// How many bars to display
	float mBarWidth;	// Display width of 1 bar
	float mBarHeight;	// How tall is each bar
	float mBarSpace;	// Width of the space between bars
	float mInstWidth;	// Total width of the instrument
	float mInstHeight;	// Total height of the instrument
	float mInstTop;		// The top line of the CDI
	float mInstLeft;	// Left position of the CDI
	double mConvertToFt; // Factor to convert units to feet
	boolean mShow;
	
	// Calc'd instrument values
	int		mDspOffset;		// Display offset of pointer
	double 	mGlideSlope;	// Vertical glideslope angle
	int		mBackColor;	// Background color of inst

	final int mColorLow = Color.RED;
	final int mColorHigh = Color.rgb(0xEE, 0xEE, 0x00); // YELLOW
	final int mColorOn   = Color.BLACK;

	/***
	 * Course Deviation Indicator
	 */
	public VASI() {
	    mConvertToFt = Preferences.feetConversion;
	}
	
	/***
	 * Allocate the paint object and figuring out how
	 * large the instrument is on the display based upon the text size in the 
	 * source paint object
	 * @param textPaint Used to figure out our overall size
	 */
	public void setSize(Paint textPaint)
	{
		// A total of 9 bars
		mBarCount = 9;
		
		// The width of each bar is the basis for the entire instrument size
		mBarWidth = textPaint.getTextSize() * (float) 0.6;
		
		// Height is 1/3 of the width
		mBarHeight = mBarWidth / 3;
		
		// Space between bars is 3x the height of the bar
		mBarSpace = 3 * mBarHeight;
		
		// Height of the entire instrument
		mInstHeight = mBarCount * mBarHeight + 	// The bars themselves
				(mBarCount - 1) * mBarSpace + 	// Space between the bars
				2 * mBarHeight;					// Space to the top and bottom of the end bars

		// Width of the entire instrument
		mInstWidth = mBarWidth + 				// The main bar itself
				mBarWidth / 2 + 				// The triangle indicator height
				mBarHeight * 3;					// Border padding at left/right/middle
		
		// Allocate the paint object to save time during draw cycle
		mVASIPaint = new Paint(textPaint);
	}

	/***
	 * Draw the instrument on the canvas given the current screen size
	 * This is called during the screen refresh/repaint - be quick about it; no "new" calls.
	 * @param canvas What to draw upon
	 * @param screenX Total width of the display canvas
	 * @param screenY Total height of the display canvas
	 * @param dest The target destination
	 */
	public void drawVASI(Canvas canvas, float screenX, float screenY, Destination dest)
	{
		// If we have no destination set, then do not draw anything
		if(dest == null || (!mShow)) {
		    mShow = false;
			return;
		}
		
		// If we are more than 30 miles from our target, then do not
		// draw anything
		double destDist = dest.getDistance(); 
		if(destDist > 20) {
		    mShow = false;
			return;
		}

		// Calculate the top position of the instrument
        mInstTop = (screenY - mInstHeight) / 2;

        // Now the left side
        mInstLeft  = screenX - mInstWidth; // Right side of display
	    
        // Draw the background
	    mVASIPaint.setColor(mBackColor);// Color
	    mVASIPaint.setAlpha(0x5F);		// Make it see-thru
	    mVASIPaint.setStrokeWidth(mInstWidth);	// How tall the inst is
	    mVASIPaint.setStyle(Paint.Style.STROKE);	// Type of brush
	    
	    // Draw the background of the instrument. This is a vertical swipe top to bottom.
        canvas.drawLine(mInstLeft, mInstTop, mInstLeft, mInstTop + mInstHeight, mVASIPaint);

        // Draw all of the horizontal bars
	    mVASIPaint.setColor(Color.WHITE);		// white
	    mVASIPaint.setStrokeWidth(mBarHeight);	// height of each bar
	    for(int idx = 0; idx < mBarCount; idx++) {
	    	float barTop = mInstTop + mBarHeight * (float) 1.5 + 
	    			idx * (mBarHeight + mBarSpace);
	        canvas.drawLine(mInstLeft + mBarHeight - mInstWidth / 2, barTop, 
	        		mInstLeft + mBarHeight + mBarWidth - mInstWidth / 2, barTop, mVASIPaint);
	    }
	    
	    // Now draw the needle indicator at the vertical center adjusted by the offset
	    drawIndicator(canvas, screenY / 2 + mDspOffset);
	}

	/***
	 * Draw the triangle position indicator to the right side of the bars
	 * @param canvas what to draw upon
	 * @param posY up/down of center position of the pointer
	 */
	private void drawIndicator(Canvas canvas, float posY)
	{
		// Left point
	    float X1 = mInstLeft + mBarHeight;
	    float Y1 = posY;
	    
	    // Top right point
	    float X2 = X1 + mBarWidth / 2;
	    float Y2 = Y1 - mBarWidth / 4;
	    
	    // Bottom right point
	    float X3 = X1 + mBarWidth / 2;
	    float Y3 = Y1 + mBarWidth / 4;

	    mVASIPaint.setColor(Color.WHITE);	// white
	    mVASIPaint.setStrokeWidth(5);		// Line width
	    
	    // Draw
	    canvas.drawLine(X1, Y1, X2, Y2, mVASIPaint); // Top leg
	    canvas.drawLine(X2, Y2, X3, Y3, mVASIPaint); // Right Leg
	    canvas.drawLine(X3, Y3, X1, Y1, mVASIPaint); // Bottom leg
		
	}

	/***
	 * Calculate the glideslope given our current position/altitude
	 * and the position/elevation of the target airport
	 * @param gpsParams where we are
	 * @param dest what our destination is
	 */
	public void calcGlideSlope(GpsParams gpsParams, Destination dest)
	{
		// Show no vertical offset by default
		
		mDspOffset = 0;
		mBackColor = mColorOn;
		
		// If either of these objects are null, there is nothing
		// we can do
		if(dest == null || gpsParams == null) {
		    mShow = false;
			return;
		}
		
		// Fetch the elevation of our destination. If we can't find it
		// then we don't want to display any vertical information
		double destElev = dest.getElevation();
		if(destElev == -200) {
		    mShow = false;
			return;
		}
		
		// Calculate our relative AGL compared to destination. If we are 
		// lower then no display info
		double relativeAGL = gpsParams.getAltitude() - destElev;
		if(relativeAGL < 0) {
            mShow = false;
			return;
		}
		
		// Convert the destination distance to feet.
		double destDist = dest.getDistance(); 
		double destInFeet = mConvertToFt * destDist;

		// Figure out our glide slope now based on our AGL height and distance
		mGlideSlope = Math.toDegrees(Math.atan(relativeAGL / destInFeet));

		// Set the color of the glide slope background. According to the AIM,
		// ON the glide slope is 2.5 to 3.1 degrees
		if(mGlideSlope < 2.5) {
			mBackColor = mColorLow;
		} else if(mGlideSlope > 3.1) {
			mBackColor = mColorHigh;
		}
			
		// Calculate the vertical display offset of the indicator
		// Anything greater/equal to 4 pegs at the top
		// Anything less/equal to 2 pegs at the bottom
		// all others scale in between based upon instrument height
		double fullDeflection = mInstHeight / 2 - mBarHeight * 1.5;
		if(mGlideSlope >= 4) {
			mDspOffset = -(int)fullDeflection;
		} else if(mGlideSlope <= 2) {
			mDspOffset = (int)fullDeflection;
		} else {
			mDspOffset = -(int)((mGlideSlope - 3) * (fullDeflection));
		}
		mShow = true;
	}
}
