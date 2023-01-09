## 2D Image - Description of plugins

For binary images black pixels are the background. White pixels are the foreground or the object. 

### Image opener
- Opens a single image or an image stack
- 8-bit grey or RGB color images
- For a stack, all images must be of same size and type  
- Includes a preview (thumbnail) panel
- Note: Fiji displays RGB images as 3 channel color images. Workaround: Type Image/Type/RGB Color 
- Note: Fiji sometimes displays RGB images with an enhanced red channel. Workaround: Image/Color/Arrange Channels... and press OK

### Image generator
- Generates a single image or an image stack
- 8-bit grey or RGB color images
- Image size can be set
- Maximal grey values can be set
- Random, Gaussian, Sine - radial, Sine - horizontal, Sine - vertical,  Constant
  - Frequency can be set for Sine
- Fractal surface - Fourier (FFT) or Midpoint displacement (MPD) or Sum of sine method
  - Theoretical fractal dimension in the range of [2,3] can be set
  - For Sum of sine the frequency, amplitude and number of iterations can be set
- Hierarchical random maps
  - Three probabilities can be set
- Fractal random shapes
  - The number of shapes can be set
  - The size (thickness/radius/size) of random shapes can be set
  - The hyperbolic downscaling [0, 1] of the size can be set (scaling=0... without downscaling, scaling=1... maximal downscaling)  
- Fractal Iterated function system (IFS) - Menger, Sierpinski, Mandelbrot islands/lakes,  Koch snowflake, Fern, Heighway dragon
  - The number of IFS iterations can be set
  - The polygon number for the Koch snowflake can be set
  - The number of iterates must be really high for the Fern 
- Note: Fiji sometimes displays oversaturated grey values. Workaround: Image/Color/Edit LUT... and press 2x Invert
- Note: Fiji displays RGB images as 3 channel color images. Workaround: Image/Type/RGB Color 
- Note: Fiji sometimes displays an enhanced R channel. Workaround: Image/Color/Arrange Channels... and press OK

### Filter
- Image filtering
- 8-bit grey or RGB color images
- Gaussian blur
  - Sigma can be set
- Mean, Median
  - Size of kernel can be set
- Low-pass, High-pass with FFT
  - Radius (Cutoff frequency) can be set

### Noise
- Adding noise
- 8-bit grey or RGB color images
- Shot, Salt&Pepper
  - Percentage of pixels that will be changed can be set
- Uniform
  - Percentage of maximum value changes can be set
- Gaussian, Rayleigh, Exponential
  - Scaling parameter can be set

### Surrogates
- Computes surrogate images
- 8-bit grey or RGB color images
- Shuffle, Gaussian, Random phase, AAFT
- FFT windowing can be set

### Box counting dimension
- Fractal dimension is computed with box counting
- 8-bit binary or grey images
- Binary [0, >0] counting or DBC and RDBC
- Raster box scanning
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref.: Sarkar & Chauduri, Pattern Recognit., 1992, [DOI 10.1016/0031-3203(92)90066-R](https://doi.org/10.1016/0031-3203(92)90066-R)
- Jin et al., Pattern Recognit. Lett., 1995, [DOI  10.1016/0167-8655(94)00119-N](https://doi.org/10.1016/0167-8655(94)00119-N)

### Pyramid dimension
- Fractal dimension is computed by using image pyramids
- 8-bit binary images
- Binary [0, >0] algorithm
- Linear regression parameters of the double log plot can be set
- Number of object pixels is counted for subsequently size reduced images
- Results are identical to the common Box Counting algorithm for quadratic images with size 2^n
- For other sizes it yields more reliable results, because box truncation is not necessary
- Ref.: Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, [DOI 10.1063/1.4958709](https://doi.org/10.1063/1.4958709)

### Minkowski dimension
- Fractal dimension is computed with morphological dilations and erosions
- 8-bit binary or grey images
- Binary [0, >0] dilation
- Blanket or Variation method with grey value dilation/erosion
- The number of dilation/erosion steps can be set
- The shape of the morphological structuring element can be set
- Linear regression parameters of the double log plot can be set 
- Ref.: Peleg et al., IEEE-TPAMI, 1984, [DOI 10.1109/TPAMI.1984.4767557](https://doi.org/10.1109/TPAMI.1984.4767557)

### Correlation dimension
- Fractal correlation dimension is computed
- 8-bit binary or grey images
- Binary [0, >0] or grey value mass algorithm
- Raster box scanning or
- Classical pair wise occurrence counting (Sliding box scanning)
- Computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixels)
- or by using a fixed grid estimation of summing up squared counts (Raster box scanning)
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Grassberger & Procaccia, Physica D, 1983, [DOI 10.1016/0167-2789(83)90298-1](https:/doi.org/10.1016/0167-2789(83)90298-1)

### Directional correlation dimension
- A directional dependent fractal correlation dimension is computed
- 8-bit binary or grey images
- Binary [0, >0] or grey value mass algorithm
- Classical pair wise occurrence counting
- Directions can be set
- Horizontal & vertical, 4 radial directions [0-180°], 180 radial directions [0-180°] 
- Computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixels)
- The number of distances with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 

### Generalized dimensions
- Generalized fractal dimensions are computed
- 8-bit binary or grey images
- Binary [0, >0] or grey value mass algorithm
- Raster or sliding box scanning
- NOTE: Fast sliding box using convolution ist still in beta
- Sliding box computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixels)
- Dq and f-spectrum plots can be shown
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Ahammer et al., Physica D, 2003, [DOI 10.1016/S0167-2789(03)00099-X](https://doi.org/10.1016/S0167-2789(03)00099-X)

### Higuchi dimension 1D
- Fractal dimension is computed for 1D grey value profiles extracted from an image
- 8-bit grey images
- Several extraction methods can be chosen
- Single centered row/column, Single meander row/column, Mean of all rows/columns, 4 radial lines [0-180°], 180 radial lines [0-180°] 
- Radial lines (grey value profiles) are length corrected and grey values are interpolated
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer, PLoS ONE, 2011, [DOI 10.1371/journal.pone.0024796](https://doi.org/10.1371/journal.pone.0024796)

### Higuchi dimension 2D
- Fractal dimension is computed with Higuchi inspired 2D algorithms
- 8-bit grey or RGB color images
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer et al., Chaos, 2015, [DOI 10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### FFT dimension
- Fractal dimension is computed with FFT algorithm
- 8-bit grey images
- Several windowing filters can be set
- Circluar average of k values or
- Mean of separate line scans (horizontal and verical) or
- Integrated line scans (k should be restricted to low values - frequencies)
- Dt=2, Topological dimension is assumed to be 2
- Linear regression parameters of the double log plot can be set
- For circular averaging, the number of regression points is higher than k itself and additionally, will be automatically lowered to the number of averages.  
- Ref.:  Anguiano et al., Journal of Microscopy, 1993, [DOI 10.1111/j.1365-2818.1993.tb03416.x](https://doi.org/10.1111/j.1365-2818.1993.tb03416.x)

### Tug of war dimension
- Fractal dimension is computed by using a tug of war algorithm
- 8-bit binary images
- Binary [0, >0] algorithm
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5  
- But it is recommended to set accuracy and confidence as high as computation times allow
- Ref.: Reiss et al., Chaos, 2016, [DOI 10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### Lacunarity
- Lacunarity is computed of a binary image  
- 8-bit binary or grey images
- The number of boxes with distinct sizes can be set
- Shows a double logarithmic plot of lacunarities
- Raster/Sliding box scanning or Tug of war method
- Binary [0, >0] algorithm for Raster/Sliding box and Tug of war method
- Grey value algortihm for Raster/Sliding box
- Sliding box computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixel)
- \<L\>-R&P... Weighted mean lacunarity according to Roy & Perfect, Fractals, 2014, [DOI10.1142/S0218348X14400039](https://doi.org/10.1142/S0218348X14400039)
- \<L\>-S&V... Weighted mean lacunarity according to Sengupta & Vinoy, Fractals, 2006, [DOI 10.1142/S0218348X06003313](https://doi.org/10.1142/S0218348X06003313)
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5  
- But it is recommended to set accuracy and confidence as high as computation times allow
- Ref.: Reiss et al., Chaos, 2016, [DOI 10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### Succolarity
- Succolarity is computed by flooding the black pixels of a binary image  
- 8-bit binary images
- Binary [0, >0] algorithm
- The number of boxes with distinct sizes can be set
- Shows a double logarithmic plot of succolarities 
- Raster box or Sliding box scanning
- Binary [0, >0] algorithm
- Flooding of image can be set to Top2down, Down2top, Left2right, Right2left or the mean of all four directions
- Ref.: de Melo & Conci, Telecommunication Systems, 2013, [DOI 10.1007/s11235-011-9657-3](https://doi.org/10.1007/s11235-011-9657-3)

### Fractal fragmentation indices
- FFI -  Fractal fragmentation index
- FFDI - Fractal fragmentation and disorder index
- FTI -  Fractal tentacularity index
- 8-bit binary images
- Binary [0, >0] algorithm
- FFI = FD of mass - FD of boundary
- FFI is the fractal dimension of the image - Fractal dimension of the boundary image
- Boundary image = Image - Eroded image (erosion by one pixel)
- FFDI = D1(1-FFI)
- D1 is the Information dimension
- FTI = FFI(convex hull) - FFI
- Fractal dimension computations with Box counting (raster box scanning)
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Andronache et al., Chaos, Solitons & Fractals, 2016, [DOI 10.1016/j.chaos.2016.06.013](https://doi.org/10.1016/j.chaos.2016.06.013)

### Generalised entropies
- Computes generalised entropies
- SE, H1, H2, H3, Renyi, Tsallis, SNorm, SEscort, SEta, SKappa, SB, SBeta, SGamma
- Probabilities are computed with plain pixel grey values
- 8-bit grey images
- A plot of Renyi entropies can be shown
- Ref.: Amigo et al., 2018, Entropy, [DOI 10.3390/e20110813](https://doi.org/10.3390/e201108), Tsallis, Introduction to Nonextensive
Statistical Mechanics, Springer, 2009

### Kolmogorov complexity and Logical depth
- KC is estimated in a fast way by compressing data bytes (ZIP, ZLIB, GZIB)
- or
- KC is estimated by the memory size of compressed images saved to disk (TIFF-LZW, PNG, J2K, JPG) - slow!
- 8-bit grey images
- RGB color images may also work, but not tested
- Lossless and lossy algorithms can be chosen
- Lossless algorithms are recommended.
- LD is estimated by the opening time of the compressed image (LZW, PNG, J2K, JPG)
- LD is estimated by the decompression time of the compressed data bytes (ZLIB, GZIB)
- Iterations should be set to as high a value as possible.
- LD values should be taken with caution, as computers are not well suited to measure times
- Ref.: Zenil et al., Complexity, 2012, [DOI 10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)
