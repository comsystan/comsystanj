<!-- ![Image](comsystan.png)-->
<img src="comsystan.png" width=100 height=100/><b>ComsystanJ<b/>

ComsystanJ (Complex Systems Analysis for ImageJ) is a collection of ImageJ2 plugins to compute the complexity, fractal dimension and other nonlinear measures of images and signals. Copyright (c) Comsystan Software

This projected is maintained by Helmut Ahammer

## Project discription

### Image(2D) - Short description of plugins

**Fractal surface creation**
- creates 2D fractal grey value surfaces (images).
- Theoretical fractal dimension can be  set.
- Fast Fourier Transformation FFT method is supported.
- Midpoint displacement MPD method is supported.

**Kolmogorov complexity and Logical Depth**
- KC is estimated by the memory size of compressed images saved to disk.
- for 8-bit grey value images.
- RGB color images may also work, but not tested.
- lossless and lossy algorithms can be chosen.
- lossless algorithms are recommended.
- LD is estimated by the opening time of the compressed image.
- Iterations should be set to as high a value as possible.
- LD values should be taken with caution, as computers are not well suited to measure times. 
- Ref.: Zenil etal., Complexity, 2012, DOI 10.1002/cplx.20388

**Fractal Dimension - Higuhi1D**
- Fractal dimensions are computed for 1D grey value profiles extracted from an image.
- uses grey values of 8-bit images.
- Several extraction methods can be chosen.
- Angle extractions use the Bresenham algorithm that introduce some interpolation errors.
- Ref.: Ahammer, PLoS ONE, 2011, DOI 10.1371/journal.pone.0024796

**Fractal Dimension - Higuchi2D**
- Fractal dimension is computed with Higuchi inspired 2D algorithms.
- uses grey values of 8-bit images.
- several options can be chosen.
- Ref.: Ahammer etal., Chaos, 2015, DOI 10.1063/1.4923030

**Fractal Dimension  - Pyramid algorithm**
- Fractal dimension is computed by using image pyramids.
- for binary 8-bit images.
- number of  object pixels is counted for subsequently size reduced images.
- Results are identical to the common Box Counting algorithm for quadratic images with size 2^n.
- For other sizes it yields more reliable results, because box truncation is not necessary.
- Ref.: Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, DOI 10.1063/1.4958709   

### Image(3D) - Short description of plugins
still to come

### Signal - Short description of plugins
still to come
